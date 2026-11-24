package com.example.data.repository

import android.util.Log
import android.os.SystemClock
import com.example.data.remote.LocalSetupTransport
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.example.data.auth.AuthRepository
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.example.data.model.BabyProfile
import com.example.data.model.DeviceState
import com.example.data.model.DiaperChange
import com.example.data.model.DiaperCurrentState
import com.example.data.model.DiaperEvent
import com.example.data.model.EstadoAtualFralda
import com.example.data.model.EstadoDispositivo
import com.example.data.model.EstadoFralda
import com.example.data.model.EventoFralda
import com.example.data.model.ObservacaoCuidador
import com.example.data.model.ReportPeriod
import com.example.data.model.ReportSummary
import com.example.data.model.TipoEvento
import com.example.data.model.TrocaFralda
import com.example.data.remote.Esp32ApiService
import com.example.data.remote.WifiSetupRequestDto
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Implementação Real do Repositório MOMO com comunicação ESP32-C6 e Firebase Realtime Database.
 *
 * Arquitetura de Comunicação (Conforme especificação PROMPT_AI_STUDIO.md):
 * 1. Pareamento inicial (SoftAP):
 *    - O smartphone conecta na rede SoftAP temporária do ESP32 ("MOMO-SETUP-0428").
 *    - O aplicativo envia POST http://192.168.4.1/wifi-setup contendo ssid, senha, ownerUid e idDispositivo.
 * 2. Uso Normal (Nuvem):
 *    - O ESP32-C6 conecta ao Wi-Fi com internet e envia sua telemetria HTTPS diretamente ao Firebase RTDB.
 *    - O aplicativo Android escuta em tempo quase real deviceStatus/{uid} via Firebase SDK.
 *    - Os comandos CALIBRAR e TROCA são gravados em deviceCommands/{uid} autenticados.
 *    - As trocas confirmadas pelo cuidador são persistidas em users/{uid}/confirmedChanges.
 *    - O histórico e os relatórios PDF utilizam exclusivamente os dados confirmados pelo cuidador.
 */
class HttpMomoRepositoryImpl(
    private var esp32IpAddress: String = "192.168.4.1",
    private val databaseProvider: () -> FirebaseDatabase? = {
        runCatching {
            Firebase.database
        }.getOrNull()
    }
) : MomoRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + Job())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

    private var apiService: Esp32ApiService = criarRetrofitService(esp32IpAddress)

    private val database: FirebaseDatabase?
        get() = databaseProvider()

    private var currentActiveUid: String? = null
    private var deviceStatusListener: ValueEventListener? = null
    private var deviceLinksListener: ValueEventListener? = null
    private var sensorEventsListener: ValueEventListener? = null
    private var sensorEvents = emptyList<DiaperEvent>()
    private val savingEvents = mutableSetOf<Pair<String, String>>()
    private val acknowledgedEvents = mutableMapOf<Pair<String, String>, TipoEvento>()
    private var confirmedChangesListener: ValueEventListener? = null

    @Volatile private var lastReceivedStatusTimestamp: Long = 0L
    private var expectedDeviceId: String? = null
    private var setupStartedElapsedMillis: Long = 0L
    private val serverClock = ServerTimeEstimator(elapsedMillis = { SystemClock.elapsedRealtime() })
    private fun elapsedMillis(): Long = SystemClock.elapsedRealtime()
    private val confirmationMutex = Mutex()
    private var linkedDeviceId: String? = null
    private var latestStatus: DataSnapshot? = null
    private val lastCommandAck = MutableStateFlow<String?>(null)
    private val commandMutex = Mutex()
    @Volatile private var cloudConnected = false
    private var pendingChangeKey: String? = null

    var ultimoErroWifiSetup: String? = null
        private set

    var ultimoErroFirebase: String? = null
        private set

    private val _babyProfile = MutableStateFlow<BabyProfile?>(null)

    private val _deviceState = MutableStateFlow(
        EstadoDispositivo(
            idDispositivo = "—",
            nomeModelo = "Nenhum dispositivo MOMO pareado",
            pareado = false,
            online = false,
            isDemo = false,
            wifiSsid = "—",
            calibrado = false,
            horarioUltimaComunicacao = "—",
            versaoFirmware = "—",
            enderecoIp = esp32IpAddress,
            modoOperacao = "REAL_ESP32"
        )
    )

    private val _diaperCurrentState = MutableStateFlow(
        EstadoAtualFralda(
            estado = EstadoFralda.AGUARDANDO_FRALDA,
            contadorFezes = 0,
            timestampUltimaTrocaMillis = 0L,
            timestampUltimoEventoMillis = 0L,
            descricaoUltimoEvento = "Aguardando conexão com o monitor MOMO",
            classificacaoAutomaticaAtiva = null,
            trocaRecomendada = false,
            calibrando = false,
            progressoCalibracao = 0,
            aguardandoNovaFralda = true,
            textoAtualizadoEm = "Sem comunicação",
            cicloFralda = 1
        )
    )

    private val _events = MutableStateFlow<List<EventoFralda>>(emptyList())
    private val _changes = MutableStateFlow<List<TrocaFralda>>(emptyList())
    private val _onboardingCompleted = MutableStateFlow(true)

    init {
        // Inicializa observação do usuário Firebase Auth para ligar listeners em tempo real
        repositoryScope.launch {
            runCatching {
                val auth = Firebase.auth
                auth.addAuthStateListener { firebaseAuth ->
                    val uid = firebaseAuth.currentUser?.uid
                    if (uid != currentActiveUid) {
                        configurarListenersFirebase(uid)
                    }
                }
            }
        }

        database?.getReference(".info/serverTimeOffset")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val offset = (snapshot.value as? Number)?.toLong() ?: return
                serverClock.updateOffset(offset)
                latestStatus?.let { processarDeviceStatusSnapshot(it) }
            }
            override fun onCancelled(error: DatabaseError) { failFirebase(error) }
        })

        database?.getReference(".info/connected")?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cloudConnected = snapshot.getValue(Boolean::class.java) == true
                if (cloudConnected) latestStatus?.let { processarDeviceStatusSnapshot(it) }
                else {
                    _deviceState.value = _deviceState.value.copy(online = false, calibrado = false, connectionMessage = "O aplicativo está sem conexão com o Firebase")
                    _diaperCurrentState.value = EstadoAtualFralda(estado = EstadoFralda.DESCONECTADO, textoAtualizadoEm = "Sem conexão com Firebase")
                }
            }
            override fun onCancelled(error: DatabaseError) { failFirebase(error) }
        })

        // Watchdog de telemetria: monitora frescor da comunicação a cada segundo
        repositoryScope.launch {
            while (true) {
                delay(1_000L)
                verificarFreshnessStatus()
            }
        }
    }

    private fun verificarFreshnessStatus() {
        if (_deviceState.value.online) {
            val now = serverClock.nowMillis() ?: return
            if (!TelemetryPolicy.isRecent(lastReceivedStatusTimestamp, now)) {
                Log.d(TAG, "Telemetria do ESP32 expirada (>=30s). Marcando dispositivo como OFFLINE.")
                _deviceState.value = _deviceState.value.copy(online = false, calibrado = false, connectionMessage = "Sem sinal recente do ESP32; última atualização expirou")
                if (_diaperCurrentState.value.estado != EstadoFralda.DESCONECTADO) {
                    _diaperCurrentState.value = _diaperCurrentState.value.copy(
                        estado = EstadoFralda.DESCONECTADO,
                        calibrando = false, progressoCalibracao = 0,
                        classificacaoAutomaticaAtiva = null, trocaRecomendada = false, c0 = null, r5s = null,
                        textoAtualizadoEm = "Sem comunicação recente"
                    )
                }
            }
        }
    }

    fun atualizarIpEsp32(novoIp: String) {
        esp32IpAddress = novoIp
        apiService = criarRetrofitService(novoIp)
        _deviceState.value = _deviceState.value.copy(enderecoIp = novoIp)
    }

    private fun criarRetrofitService(ip: String): Esp32ApiService {
        val baseUrl = if (ip.startsWith("http://") || ip.startsWith("https://")) {
            if (ip.endsWith("/")) ip else "$ip/"
        } else {
            "http://$ip/"
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(com.squareup.moshi.Moshi.Builder().addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()))
            .build()

        return retrofit.create(Esp32ApiService::class.java)
    }

    /**
     * Configura ouvintes em tempo quase real no Firebase Realtime Database para o UID autenticado:
     * - deviceLinks/{uid} : Vínculo e identificação do ESP32-C6 cadastrado
     * - deviceStatus/{uid} : Estado operacional e telemetria reportada pelo ESP32-C6
     * - users/{uid}/confirmedChanges : Histórico de trocas confirmadas pelo cuidador
     */
    private fun configurarListenersFirebase(uid: String?) {
        val db = database ?: return

        // Remove listeners antigos se houver
        deviceLinksListener?.let {
            currentActiveUid?.let { oldUid ->
                db.reference.child("deviceLinks").child(oldUid).removeEventListener(it)
            }
        }
        deviceStatusListener?.let {
            currentActiveUid?.let { oldUid ->
                db.reference.child("deviceStatus").child(oldUid).removeEventListener(it)
            }
        }
        confirmedChangesListener?.let {
            currentActiveUid?.let { oldUid ->
                db.reference.child("users").child(oldUid).child("confirmedChanges").removeEventListener(it)
            }
        }

        currentActiveUid?.let { oldUid ->
            sensorEventsListener?.let {
                db.reference.child("users").child(oldUid).child("sensorEvents").removeEventListener(it)
            }
        }
        sensorEvents = emptyList()
        savingEvents.clear()
        acknowledgedEvents.clear()
        currentActiveUid = uid
        linkedDeviceId = null
        latestStatus = null
        lastReceivedStatusTimestamp = 0L
        pendingChangeKey = null
        lastCommandAck.value = null
        expectedDeviceId = null
        _babyProfile.value = null
        _events.value = emptyList()
        _changes.value = emptyList()
        _deviceState.value = EstadoDispositivo()
        _diaperCurrentState.value = EstadoAtualFralda(estado = EstadoFralda.DESCONECTADO)

        if (uid.isNullOrBlank()) {
            _deviceState.value = _deviceState.value.copy(
                pareado = false,
                online = false,
                nomeModelo = "Nenhum dispositivo MOMO pareado"
            )
            return
        }

        // 1. Ouvir deviceLinks/{uid} (Vínculo do hardware com o usuário Google)
        val linksListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (uid != currentActiveUid) return
                val id = snapshot.child("deviceUid").getValue(String::class.java)
                val owner = uid
                linkedDeviceId = id?.takeIf { it.isNotBlank() && owner == uid }
                _deviceState.value = EstadoDispositivo(
                    idDispositivo = linkedDeviceId ?: "—",
                    nomeModelo = if (linkedDeviceId == null) "Nenhum dispositivo MOMO pareado" else "Monitor MOMO ESP32-C6",
                    connectionMessage = if (linkedDeviceId == null) "Nenhum vínculo para a conta Google conectada" else "Vínculo confirmado; aguardando sinal do ESP32",
                    pareado = linkedDeviceId != null
                )
                latestStatus?.let { processarDeviceStatusSnapshot(it) }
            }

            override fun onCancelled(error: DatabaseError) {
                if (uid == currentActiveUid) failFirebase(error)
            }
        }
        deviceLinksListener = linksListener
        db.reference.child("deviceLinks").child(uid).addValueEventListener(linksListener)

        // 2. Ouvir deviceStatus/{uid} (ESP32 -> Firebase -> App)
        val statusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (uid != currentActiveUid) return
                latestStatus = snapshot
                if (snapshot.exists()) {
                    processarDeviceStatusSnapshot(snapshot)
                } else {
                    _deviceState.value = _deviceState.value.copy(online = false, calibrado = false, connectionMessage = "Ainda não há sinal do ESP32 para a conta conectada")
                    _diaperCurrentState.value = EstadoAtualFralda(estado = EstadoFralda.DESCONECTADO, textoAtualizadoEm = "Aguardando dispositivo")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (uid == currentActiveUid) failFirebase(error)
            }
        }
        deviceStatusListener = statusListener
        db.reference.child("deviceStatus").child(uid).addValueEventListener(statusListener)

        // 3. Ouvir users/{uid}/confirmedChanges (Trocas confirmadas pelo cuidador)
        val changesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (uid == currentActiveUid) processarConfirmedChangesSnapshot(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                if (uid == currentActiveUid) { _changes.value = emptyList(); refreshSensorEvents(); failFirebase(error) }
            }
        }
        confirmedChangesListener = changesListener
        db.reference.child("users").child(uid).child("confirmedChanges").addValueEventListener(changesListener)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (uid != currentActiveUid) return
                sensorEvents = snapshot.children.mapNotNull { event ->
                    val id = event.key ?: return@mapNotNull null
                    val type = predictionType(event.child("predictedClass").value as? String)
                        ?: return@mapNotNull null
                    val timestamp = (event.child("telemetryTimestamp").value as? Number)?.toLong() ?: 0L
                    val source = event.child("source").value as? String ?: return@mapNotNull null
                    if (source != "reconstructed_test" && (source != "live" || timestamp <= 0L)) return@mapNotNull null
                    val detectedAt = (event.child("detectedAt").value as? Number)?.toLong()?.takeIf { it > 0L }
                    val trialReferenceAt = (event.child("caregiverRecordedAtMillis").value as? Number)?.toLong()?.takeIf { it > 0L }
                    val observation = ObservacaoCuidador.fromString(event.child("reportedObservation").value as? String)
                    EventoFralda(
                        id, timestamp, type, predictionTitle(type),
                        if (source == "reconstructed_test") "Reconstrução do teste integrado" else "Classificação automática do sensor",
                        "", "", classificacaoAutomatica = type.name,
                        observacaoCuidador = observation, source = source,
                        detectedAt = detectedAt, trialReferenceAt = trialReferenceAt
                    )
                }.sortedByDescending { it.historyTimestamp }
                refreshSensorEvents()
            }

            override fun onCancelled(error: DatabaseError) {
                if (uid == currentActiveUid) {
                    sensorEvents = emptyList()
                    refreshSensorEvents()
                    failFirebase(error)
                }
            }
        }
        sensorEventsListener = listener
        db.reference.child("users").child(uid).child("sensorEvents").addValueEventListener(listener)
    }

    /**
     * Processa a telemetria enviada pelo ESP32-C6 em deviceStatus/{uid}.
     */
    private fun failFirebase(error: DatabaseError) {
        ultimoErroFirebase = "Firebase: ${error.message}"
        _deviceState.value = _deviceState.value.copy(online = false, calibrado = false, connectionMessage = ultimoErroFirebase!!)
        _diaperCurrentState.value = EstadoAtualFralda(estado = EstadoFralda.DESCONECTADO, textoAtualizadoEm = ultimoErroFirebase!!)
    }

    private fun processarDeviceStatusSnapshot(snapshot: DataSnapshot) {
        try { applyDeviceStatusSnapshot(snapshot) }
        catch (e: Exception) {
            ultimoErroFirebase = "Telemetria inválida: ${e.message}"
            _deviceState.value = _deviceState.value.copy(online = false, calibrado = false, connectionMessage = ultimoErroFirebase!!)
            _diaperCurrentState.value = EstadoAtualFralda(estado = EstadoFralda.DESCONECTADO, textoAtualizadoEm = ultimoErroFirebase!!)
        }
    }

    private fun applyDeviceStatusSnapshot(snapshot: DataSnapshot) {
        val now = serverClock.nowMillis()

        val estadoStr = snapshot.child("estado").getValue(String::class.java) ?: "DESCONECTADO"
        val conectado = snapshot.child("conectado").getValue(Boolean::class.java) ?: false
        val calibrado = snapshot.child("calibrado").getValue(Boolean::class.java) ?: false
        val progressoCal = snapshot.child("progressoCalibracao").getValue(Long::class.java)?.toInt()
            ?: snapshot.child("progressoCalibracao").getValue(Int::class.java) ?: 0
        val ultimaClass = snapshot.child("ultimaClassificacaoAutomatica").getValue(String::class.java)
            ?: snapshot.child("classificacaoAutomatica").getValue(String::class.java)
        val trocaRecomendada = snapshot.child("trocaRecomendada").getValue(Boolean::class.java) ?: false
        val c0 = snapshot.child("c0").getValue(Double::class.java)?.toFloat()
        val r5s = snapshot.child("r5s").getValue(Double::class.java)?.toFloat()
        val cicloFralda = snapshot.child("cicloFralda").getValue(Long::class.java)?.toInt() ?: 1
        val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
        val idDisp = snapshot.child("deviceUid").getValue(String::class.java) ?: ""
        val firmware = snapshot.child("versaoFirmware").getValue(String::class.java) ?: "—"
        val wifiSsid = snapshot.child("wifiSsid").getValue(String::class.java) ?: "—"

        if (linkedDeviceId == null || idDisp != linkedDeviceId) {
            _deviceState.value = _deviceState.value.copy(online = false, calibrado = false, connectionMessage = "O sinal recebido não corresponde ao dispositivo vinculado à conta")
            _diaperCurrentState.value = EstadoAtualFralda(estado = EstadoFralda.DESCONECTADO, textoAtualizadoEm = "Aguardando telemetria do dispositivo vinculado")
            return
        }
        require(estadoStr in listOf("CALIBRANDO", "AGUARDANDO_FRALDA", "MONITORANDO", "FRALDA_SECA", "URINA", "FEZES", "DIARREIA", "TROCA_RECOMENDADA", "DESCONECTADO")) { "Estado desconhecido." }
        require(progressoCal in 0..100 && cicloFralda > 0) { "Progresso ou ciclo inválido." }
        require(ultimaClass == null || ultimaClass in listOf("URINA", "FEZES", "DIARREIA")) { "Classificação desconhecida." }
        val estadoFralda = EstadoFralda.fromString(estadoStr)


        // Considera online somente se a flag conectado for true E houver timestamp recente (< 120 segundos)
        val isRecent = now != null && TelemetryPolicy.isRecent(timestamp, now)
        val matchesLink = linkedDeviceId != null && idDisp == linkedDeviceId
        val isDeviceOnline = cloudConnected && conectado && isRecent && matchesLink
        lastReceivedStatusTimestamp = if (isDeviceOnline) timestamp else 0L

        val horarioCom = if (timestamp > 0L) timeFormat.format(Date(timestamp)) else "—"

        _deviceState.value = _deviceState.value.copy(
            idDispositivo = idDisp,
            nomeModelo = "Monitor MOMO ESP32-C6",
            pareado = matchesLink,
            online = isDeviceOnline,
            isDemo = false,
            calibrado = calibrado && isDeviceOnline,
            wifiSsid = wifiSsid,
            versaoFirmware = firmware,
            horarioUltimaComunicacao = horarioCom,
            connectionMessage = when {
                !cloudConnected -> "O aplicativo está sem conexão com o Firebase"
                now == null -> "Sincronizando horário com o Firebase"
                !conectado -> "O ESP32 informou que o monitor ou sensor está indisponível"
                timestamp <= 0L -> "O ESP32 não informou um horário válido"
                !isRecent -> "O último sinal do ESP32 está antigo ou com horário inválido"
                else -> "Sinal real do ESP32 recebido pelo Firebase"
            }
        )

        val isCalibrando = estadoStr.equals("CALIBRANDO", ignoreCase = true) || (progressoCal in 1..99)
        val isAguardando = estadoStr.equals("AGUARDANDO_FRALDA", ignoreCase = true)

        val eventId = snapshot.child("eventoId").getValue(Long::class.java) ?: 0L
        val prediction = predictionType(ultimaClass)
        val ownerUid = currentActiveUid
        val detectionId = if (
            isDeviceOnline && !isCalibrando && !isAguardando && calibrado &&
            eventId > 0 && prediction != null && estadoStr in listOf(prediction.name, "TROCA_RECOMENDADA") &&
            snapshot.child("sensorOk").value == true && snapshot.child("erroSensor").value != true &&
            snapshot.child("eventoPendente").value != true &&
            ownerUid != null && Firebase.auth.currentUser?.uid == ownerUid
        ) com.example.data.model.SensorEventPolicy.id(idDisp, cicloFralda, eventId) else null
        val synchronizedId = detectionId?.takeIf { ownerUid != null && acknowledgedEvents[ownerUid to it] == prediction }
        _diaperCurrentState.value = _diaperCurrentState.value.copy(
            sensorEventId = synchronizedId,
            estado = if (!isDeviceOnline) EstadoFralda.DESCONECTADO else estadoFralda,
            trocaRecomendada = isDeviceOnline && trocaRecomendada,
            calibrando = isDeviceOnline && isCalibrando,
            progressoCalibracao = if (isDeviceOnline) progressoCal else 0,
            aguardandoNovaFralda = isDeviceOnline && isAguardando,
            classificacaoAutomaticaAtiva = if (isDeviceOnline) ultimaClass else null,
            c0 = if (isDeviceOnline) c0 else null,
            r5s = if (isDeviceOnline) r5s else null,
            cicloFralda = cicloFralda,
            firmwareEventId = snapshot.child("eventoId").getValue(Long::class.java) ?: 0L,
            descricaoUltimoEvento = if (isDeviceOnline && ultimaClass != null) "Detecção do ESP32: $ultimaClass" else "Aguardando detecção real",
            textoAtualizadoEm = if (timestamp > 0L) horarioCom else "Sem comunicação"
        )
        if (detectionId != null && ownerUid != null && prediction != null && synchronizedId == null) {
            persistDetection(ownerUid, detectionId, idDisp, cicloFralda, eventId, timestamp, prediction)
        }
        if (isDeviceOnline) lastCommandAck.value = snapshot.child("lastCommandId").getValue(String::class.java)

    }

    /**
     * Processa a lista de trocas confirmadas gravadas em users/{uid}/confirmedChanges.
     */
    private fun processarConfirmedChangesSnapshot(snapshot: DataSnapshot) {
        val changesList = mutableListOf<TrocaFralda>()

        for (child in snapshot.children) {
            val id = child.child("id").getValue(String::class.java) ?: child.key ?: UUID.randomUUID().toString()
            val timestamp = child.child("timestampMillis").getValue(Long::class.java) ?: 0L
            val obsStr = child.child("observacaoCuidador").getValue(String::class.java)
            val autoClass = child.child("classificacaoAutomatica").getValue(String::class.java)
            val observacoes = child.child("observacoes").getValue(String::class.java)
            val ciclo = child.child("cicloFralda").getValue(Long::class.java)?.toInt() ?: 1

            val obs = ObservacaoCuidador.fromString(obsStr)
            changesList.add(
                TrocaFralda(
                    id = id,
                    timestampMillis = timestamp,
                    observacaoCuidador = obs,
                    classificacaoAutomatica = autoClass,
                    observacoes = observacoes,
                    cicloFralda = ciclo,
                    sensorEventId = child.child("sensorEventId").value as? String
                )
            )
        }

        // Ordena cronologicamente decrescente
        _changes.value = changesList.sortedByDescending { it.timestampMillis }
        refreshSensorEvents()
    }

    private fun predictionType(value: String?): TipoEvento? = when (value) {
        "URINA" -> TipoEvento.URINA
        "FEZES" -> TipoEvento.FEZES
        "DIARREIA" -> TipoEvento.DIARREIA
        else -> null
    }

    private fun predictionTitle(type: TipoEvento): String = when (type) {
        TipoEvento.URINA -> "Urina detectada"
        TipoEvento.FEZES -> "Possível presença de fezes"
        TipoEvento.DIARREIA -> "Possível diarreia"
        else -> type.rotulo
    }

    private fun refreshSensorEvents() {
        val confirmations = _changes.value.asReversed().mapNotNull { change ->
            change.sensorEventId?.let { it to change.observacaoCuidador }
        }.toMap()
        _events.value = sensorEvents.map { event ->
            val observation = confirmations[event.id] ?: event.observacaoCuidador
            val status = com.example.data.model.SensorEventPolicy.comparison(event.tipo, observation)
            event.copy(observacaoCuidador = observation, situacao = status)
        }
    }

    private fun persistDetection(uid: String, id: String, device: String, cycle: Int, eventId: Long, timestamp: Long, type: TipoEvento) {
        val db = database ?: return
        val key = uid to id
        if (acknowledgedEvents[key] == type || !savingEvents.add(key)) return
        val reference = db.reference.child("users").child(uid).child("sensorEvents").child(id)
        reference.runTransaction(object : Transaction.Handler {
            override fun doTransaction(current: MutableData): Transaction.Result {
                if (current.value == null) {
                    current.value = mapOf(
                        "deviceUid" to device, "cicloFralda" to cycle, "eventoId" to eventId,
                        "predictedClass" to type.name, "telemetryTimestamp" to timestamp, "source" to "live"
                    )
                } else if (!com.example.data.model.SensorEventPolicy.matchesPrediction(current.child("predictedClass").value, type)) {
                    return Transaction.abort()
                }
                return Transaction.success(current)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                savingEvents.remove(key)
                if (currentActiveUid != uid || Firebase.auth.currentUser?.uid != uid) return
                if (committed && error == null) {
                    acknowledgedEvents[key] = type
                    latestStatus?.let { processarDeviceStatusSnapshot(it) }
                } else {
                    _deviceState.value = _deviceState.value.copy(
                        connectionMessage = if (error != null) "Detecção não sincronizada: ${error.message}"
                        else "Conflito no identificador da detecção. Previsão original preservada."
                    )
                }
            }
        }, false)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun getBabyProfile(): Flow<BabyProfile?> = AuthRepository.getInstance().authStateFlow().flatMapLatest { user ->
        if (user == null) flowOf(null) else FirebaseBabyProfileRepository.getInstance().observeBabyProfile(user.uid)
    }

    override suspend fun saveBabyProfile(profile: BabyProfile) {
        val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
        if (currentUser != null) {
            FirebaseBabyProfileRepository.getInstance().saveBabyProfile(currentUser.uid, profile).getOrThrow()
        }
        _babyProfile.value = profile
    }

    override fun getDeviceState(): Flow<DeviceState> = _deviceState.asStateFlow()

    override suspend fun updateDeviceState(state: DeviceState) {
        error("Estado real somente pode ser atualizado por telemetria autenticada.")
    }

    override fun getDiaperCurrentState(): Flow<DiaperCurrentState> = _diaperCurrentState.asStateFlow()

    override fun getEvents(): Flow<List<DiaperEvent>> = _events.asStateFlow()

    override fun getChanges(): Flow<List<DiaperChange>> = _changes.asStateFlow()

    /**
     * Registra troca de fralda confirmada pelo cuidador:
     * 1. Persiste a observação humana em users/{uid}/confirmedChanges/{changeId}.
     * 2. Envia o comando TROCA para deviceCommands/{uid} no Firebase RTDB para o ESP32 atuar.
     * 3. Aguarda lastCommandId; somente o firmware avança o ciclo.
     */
    override suspend fun recordDiaperChange(observation: ObservacaoCuidador, notes: String?, sensorEventId: String?) = confirmationMutex.withLock {
        val uid = Firebase.auth.currentUser?.uid ?: error("Entre com Google para salvar a confirmação.")
        val db = database ?: error("Firebase indisponível.")
        val baby = FirebaseBabyProfileRepository.getInstance().observeBabyProfile(uid).first()
            ?: error("Cadastre o bebê antes de confirmar.")
        val state = _diaperCurrentState.value
        check(_deviceState.value.pareado && _deviceState.value.online && state.cicloFralda > 0) { "Nenhum dispositivo vinculado." }
        if (sensorEventId != null) {
            check(sensorEventId == state.sensorEventId) { "A detecção mudou. Abra a confirmação novamente." }
        }
        val key = pendingChangeKey ?: ConfirmationIdentity.forCycle(_deviceState.value.idDispositivo, state.cicloFralda)
        val ref = db.reference.child("users").child(uid).child("confirmedChanges").child(key)
        pendingChangeKey = key
        if (ref.get().await().exists()) {
            sendCommand("TROCA", key)
            pendingChangeKey = null
            return@withLock
        }
        val data = mapOf(
            "id" to key, "timestampMillis" to System.currentTimeMillis(),
            "babyId" to baby.id, "idDispositivo" to _deviceState.value.idDispositivo,
            "eventoId" to state.firmwareEventId, "telemetryTimestamp" to lastReceivedStatusTimestamp,
            "sensorEventId" to sensorEventId,
            "observacaoCuidador" to observation.name,
            "classificacaoAutomatica" to (state.classificacaoAutomaticaAtiva ?: ""),
            "observacoes" to (notes ?: ""), "cicloFralda" to state.cicloFralda)
        check(Firebase.auth.currentUser?.uid == uid) { "A conta mudou durante a confirmação." }
        withTimeout(15_000L) { ref.setValue(data).await() }
        sendCommand("TROCA", key)
        pendingChangeKey = null
    }

    private suspend fun sendCommand(action: String, commandId: String = UUID.randomUUID().toString()) = commandMutex.withLock {
        val uid = Firebase.auth.currentUser?.uid ?: error("Entre com Google.")
        check(_deviceState.value.online) { "Monitor offline. Aguarde comunicação recente." }
        val db = database ?: error("Firebase indisponível.")
        withTimeout(15_000L) {
            db.reference.child("deviceCommands").child(uid).setValue(mapOf(
                "id" to commandId, "acao" to action, "timestamp" to ServerValue.TIMESTAMP)).await()
        }
        val acknowledged = withTimeoutOrNull(45_000L) {
            lastCommandAck.first { ack -> Firebase.auth.currentUser?.uid == uid && _deviceState.value.online && ack == commandId }
            true
        } ?: false
        check(acknowledged) { "Comando enviado, mas sem confirmação do ESP32. Não considere concluído." }
    }

    override suspend fun startDiaperCalibration() {
        sendCommand("CALIBRAR")
        val completed = withTimeoutOrNull(60_000L) {
            _diaperCurrentState.first { _deviceState.value.online && _deviceState.value.calibrado && !it.calibrando && it.progressoCalibracao >= 100 }
            true
        } ?: false
        check(completed) { "O ESP32 não confirmou o fim da calibração." }
    }

    override suspend fun completeCalibrationAndStartMonitoring() {
        check(_deviceState.value.online && _deviceState.value.calibrado) {
            "A calibração ainda não foi confirmada pelo ESP32."
        }
    }

    override suspend fun calibrateNewDiaper() {
        startDiaperCalibration()
    }

    /**
     * Calcula o resumo do relatório para o período selecionado utilizando
     * ESTRITAMENTE as trocas confirmadas pelo cuidador em users/{uid}/confirmedChanges.
     */
    override suspend fun getReportSummary(period: ReportPeriod): ReportSummary {
        val now = System.currentTimeMillis()
        val cutoffMillis = when (period) {
            ReportPeriod.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            ReportPeriod.DAYS_7 -> now - (7L * 24 * 3600 * 1000)
            ReportPeriod.DAYS_30 -> now - (30L * 24 * 3600 * 1000)
        }

        val periodChanges = _changes.value.filter { it.timestampMillis >= cutoffMillis }

        val changesCount = periodChanges.size
        val diapersWithUrine = periodChanges.count { it.observacaoCuidador.hasUrine }
        val feces = periodChanges.count { it.observacaoCuidador.hasFeces }
        val diarrhea = periodChanges.count { it.observacaoCuidador.hasDiarrhea }

        val avgHours = if (periodChanges.size >= 2) {
            val sortedChanges = periodChanges.sortedBy { it.timestampMillis }
            val intervals = (1 until sortedChanges.size).map { i ->
                (sortedChanges[i].timestampMillis - sortedChanges[i - 1].timestampMillis) / (1000.0 * 3600.0)
            }
            val avg = intervals.average()
            String.format(Locale.US, "%.1f", avg).toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        return ReportSummary(
            period = period,
            diapersWithUrineCount = diapersWithUrine,
            fecesEventsCount = feces,
            possibleDiarrheaCount = diarrhea,
            diaperChangesCount = changesCount,
            averageHoursBetweenChanges = avgHours
        )
    }

    /**
     * Envia o POST http://192.168.4.1/wifi-setup para o ESP32-C6 conectado em SoftAP.
     * Envia ssid, senha, ownerUid e idDispositivo.
     * Nunca registra senhas em log.
     */
    override suspend fun configurarWifi(
        ssid: String,
        senha: String,
        ownerUid: String,
        idDispositivo: String
    ): Boolean {
        val uid = ownerUid.ifBlank {
            runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: ""
        }

        ultimoErroWifiSetup = null

        return withContext(Dispatchers.IO) {
            try {
                val request = WifiSetupRequestDto(
                    ssid = ssid,
                    senha = senha,
                    ownerUid = uid,
                    idDispositivo = idDispositivo.ifBlank { "MOMO-0428" }
                )
                require(uid.isNotBlank() && uid == Firebase.auth.currentUser?.uid) { "Usuário Google inválido." }
                require(idDispositivo.isNotBlank()) { "Informe o identificador do monitor." }
                expectedDeviceId = null
                require(ssid.toByteArray(Charsets.UTF_8).size in 1..32 && senha.toByteArray(Charsets.UTF_8).size in 8..63) { "O firmware exige SSID de 1–32 bytes e senha Wi-Fi de 8–63 bytes." }
                setupStartedElapsedMillis = elapsedMillis()
                val response = LocalSetupTransport.send(request)
                if (response.isSuccessful && response.body()?.sucesso == true) {
                    val deviceUid = response.body()?.deviceUid
                    require(!deviceUid.isNullOrBlank()) { "Resposta sem deviceUid." }
                    expectedDeviceId = deviceUid
                    setupStartedElapsedMillis = elapsedMillis()
                    // Vínculo será solicitado após o celular retornar à internet.

                    true
                } else {
                    val code = response.code()
                    val msg = response.body()?.mensagem ?: response.message()
                    ultimoErroWifiSetup = "O ESP32 respondeu com erro (HTTP $code): ${msg.ifBlank { "Credenciais rejeitadas" }}"
                    Log.w(TAG, "Falha na resposta do ESP32: HTTP $code")
                    false
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: java.net.ConnectException) {
                ultimoErroWifiSetup = "Não foi possível conectar ao ESP32 em http://192.168.4.1. Verifique se o smartphone está conectado à rede Wi-Fi temporária MOMO-SETUP-0428."
                Log.e(TAG, "Erro de conexão com SoftAP: ${e.message}")
                false
            } catch (e: java.net.SocketTimeoutException) {
                ultimoErroWifiSetup = "Tempo limite esgotado ao tentar comunicar com http://192.168.4.1. Aproxime o smartphone do monitor MOMO."
                Log.e(TAG, "Timeout com SoftAP: ${e.message}")
                false
            } catch (e: Exception) {
                ultimoErroWifiSetup = "Falha na comunicação com o ESP32: ${e.localizedMessage ?: "Erro desconhecido"}"
                Log.e(TAG, "Falha na comunicação com SoftAP do ESP32 (POST /wifi-setup)", e)
                false
            }
        }
    }

    /**
     * Consulta o Firebase Realtime Database para verificar se o dispositivo vinculado
     * começou a reportar dados recentes em deviceStatus/{uid}.
     * Não utiliza timers fictícios; realiza checagem real contra o backend.
     */
    override suspend fun verificarDispositivoFirebase(timeoutSeconds: Int): Boolean {
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull()
        if (uid.isNullOrBlank()) {
            ultimoErroFirebase = "Usuário Google não autenticado."
            return false
        }
        val db = database
        if (db == null) {
            ultimoErroFirebase = "Instância do Firebase RTDB não inicializada."
            return false
        }

        ultimoErroFirebase = null
        try {
            val deviceUid = expectedDeviceId ?: error("Envie a configuração ao ESP32 primeiro.")
            val link = db.reference.child("deviceLinks").child(uid)
            withTimeoutOrNull(15_000L) {
                val existing = link.get().await().child("deviceUid").getValue(String::class.java)
                require(existing == null || existing == deviceUid) { "A conta já possui outro dispositivo vinculado." }
                require(existing == deviceUid) { "Vínculo seguro ausente. Cadastre deviceLinks/UID_DA_CONTA/deviceUid com o UID técnico do ESP32 por um administrador confiável. O app não cria vínculos sem prova de posse." }
                true
            } ?: error("Sem internet para confirmar o vínculo.")
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { ultimoErroFirebase = e.message; return false }
        val verified = withTimeoutOrNull(timeoutSeconds * 1000L) {
            _deviceState.first { device ->
                Firebase.auth.currentUser?.uid == uid && device.online && device.pareado &&
                    device.idDispositivo == expectedDeviceId && serverClock.nowMillis()?.let { serverNow ->
                        lastReceivedStatusTimestamp >= serverNow - (elapsedMillis() - setupStartedElapsedMillis)
                    } == true
            }
            true
        } ?: false
        if (verified) return true

        ultimoErroFirebase = "Ainda não detectamos o sinal do ESP32 no Firebase. Certifique-se de que reconectou seu celular à internet e que o monitor está ligado com Wi-Fi correto."
        return false
    }

    suspend fun desvincularDispositivo(): Boolean {
        val uid = runCatching { Firebase.auth.currentUser?.uid }.getOrNull() ?: return false
        val db = database ?: return false
        return try {
            db.reference.child("deviceLinks").child(uid).removeValue().await()
            _deviceState.value = EstadoDispositivo(
                idDispositivo = "—",
                nomeModelo = "Nenhum dispositivo MOMO pareado",
                pareado = false,
                online = false,
                isDemo = false,
                wifiSsid = "—",
                calibrado = false,
                horarioUltimaComunicacao = "—",
                versaoFirmware = "—",
                enderecoIp = esp32IpAddress,
                modoOperacao = "REAL_ESP32"
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao desvincular dispositivo", e)
            false
        }
    }

    override fun isOnboardingCompleted(): Flow<Boolean> = _onboardingCompleted.asStateFlow()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        _onboardingCompleted.value = completed
    }

    companion object {
        private const val TAG = "HttpMomoRepo"

        @Volatile
        private var instance: HttpMomoRepositoryImpl? = null

        fun getInstance(ipAddress: String = "192.168.4.1"): HttpMomoRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: HttpMomoRepositoryImpl(ipAddress).also { instance = it }
            }
        }
    }
}
