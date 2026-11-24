package com.example.presentation.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ReportPeriod
import com.example.data.model.ReportSummary
import com.example.data.repository.ModoRepositorio
import com.example.data.repository.MomoRepository
import com.example.data.repository.MomoRepositoryProvider
import com.example.util.PdfReportGenerator
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.DAYS_7,
    val summary: ReportSummary = ReportSummary(
        period = ReportPeriod.DAYS_7,
        diapersWithUrineCount = 0,
        fecesEventsCount = 0,
        possibleDiarrheaCount = 0,
        diaperChangesCount = 0,
        averageHoursBetweenChanges = 0.0
    ),
    val isGeneratingPdf: Boolean = false,
    val generatedPdfUri: Uri? = null,
    val showPdfExportDialog: Boolean = false,
    val errorMessage: String? = null
)

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReportsViewModel : ViewModel() {

    private val repository: MomoRepository get() = MomoRepositoryProvider.getRepository()

    private val _selectedPeriod = MutableStateFlow(ReportPeriod.DAYS_7)
    private val _summary = MutableStateFlow(
        ReportSummary(
            period = ReportPeriod.DAYS_7,
            diapersWithUrineCount = 0,
            fecesEventsCount = 0,
            possibleDiarrheaCount = 0,
            diaperChangesCount = 0,
            averageHoursBetweenChanges = 0.0
        )
    )
    private val _isGeneratingPdf = MutableStateFlow(false)
    private val _generatedPdfUri = MutableStateFlow<Uri?>(null)
    private val _showPdfExportDialog = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ReportsUiState> = combine(
        combine(_selectedPeriod, _summary, _isGeneratingPdf) { p, s, g -> Triple(p, s, g) },
        combine(_generatedPdfUri, _showPdfExportDialog, _errorMessage) { u, d, e -> Triple(u, d, e) }
    ) { (period, summary, isGenerating), (uri, isDialog, error) ->
        ReportsUiState(
            selectedPeriod = period,
            summary = summary,
            isGeneratingPdf = isGenerating,
            generatedPdfUri = uri,
            showPdfExportDialog = isDialog,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    init {
        viewModelScope.launch {
            MomoRepositoryProvider.modoAtual.flatMapLatest {
                _generatedPdfUri.value = null
                _showPdfExportDialog.value = false
                MomoRepositoryProvider.getRepository().getChanges()
            }.collectLatest { _summary.value = repository.getReportSummary(_selectedPeriod.value) }
        }
    }

    fun setPeriod(period: ReportPeriod) {
        _selectedPeriod.value = period
        loadSummary(period)
    }

    private fun loadSummary(period: ReportPeriod) {
        viewModelScope.launch {
            val res = repository.getReportSummary(period)
            _summary.value = res
        }
    }

    fun generateAndSharePdf(context: Context, includeHistory: Boolean = false) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            _errorMessage.value = null
            val exportMode = MomoRepositoryProvider.modoAtual.value
            val exportRepository = repository
            try {
                val babyProfile = exportRepository.getBabyProfile().first()
                val period = _selectedPeriod.value
                val summary = exportRepository.getReportSummary(period)
                _summary.value = summary

                val cutoff = calculateCutoff(period)
                val allChanges = exportRepository.getChanges().first()
                val periodChanges = allChanges

                val allEvents = exportRepository.getEvents().first()
                val periodEvents = allEvents

                val isDemo = exportMode == ModoRepositorio.DEMO

                val uri = withContext(Dispatchers.IO) {
                    PdfReportGenerator.generateAndGetUri(
                        context = context,
                        babyProfile = babyProfile,
                        summary = summary,
                        period = period,
                        periodChanges = periodChanges,
                        periodEvents = periodEvents,
                        isDemoMode = isDemo,
                        includeHistory = includeHistory
                    )
                }

                check(MomoRepositoryProvider.modoAtual.value == exportMode) { "O modo mudou durante a geração. Gere o relatório novamente." }
                _generatedPdfUri.value = uri
                _showPdfExportDialog.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Erro ao gerar PDF: ${e.localizedMessage}"
                Toast.makeText(context, "Erro ao gerar relatório PDF", Toast.LENGTH_SHORT).show()
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    private fun calculateCutoff(period: ReportPeriod): Long {
        val now = System.currentTimeMillis()
        return when (period) {
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
    }

    fun sharePdfUri(context: Context, uri: Uri) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Relatório MOMO - ${_selectedPeriod.value.label}")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Segue anexo o Relatório MOMO (${_selectedPeriod.value.label}) de monitoramento de fraldas."
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Compartilhar Relatório MOMO")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            _showPdfExportDialog.value = false
        } catch (e: Exception) {
            Toast.makeText(context, "Não foi possível abrir o compartilhamento", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdfExportDialog() {
        _showPdfExportDialog.value = true
    }

    fun dismissPdfExportDialog() {
        _showPdfExportDialog.value = false
    }
}

