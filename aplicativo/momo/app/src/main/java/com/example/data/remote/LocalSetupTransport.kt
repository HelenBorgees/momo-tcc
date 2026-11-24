package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.Response
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/** Somente o cliente local usa o Wi-Fi; Auth/RTDB mantêm a rota de internet. */
object LocalSetupTransport {
    private var context: Context? = null
    fun initialize(context: Context) { this.context = context.applicationContext }

    suspend fun send(request: WifiSetupRequestDto): Response<RespostaEsp32Dto> {
        val manager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: error("Serviço de rede indisponível.")
        val wifi = manager.allNetworks.filter { network ->
            manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
        require(wifi.size == 1) { "Conecte o celular somente à rede MOMO-SETUP-0428 e tente novamente." }
        val network = wifi.single()
        val client = OkHttpClient.Builder()
            .socketFactory(network.socketFactory)
            .dns(object : okhttp3.Dns { override fun lookup(hostname: String): List<java.net.InetAddress> = network.getAllByName(hostname).toList() })
            .proxy(java.net.Proxy.NO_PROXY)
            .followRedirects(false).followSslRedirects(false)
            .retryOnConnectionFailure(false)
            .connectTimeout(8, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS).callTimeout(20, TimeUnit.SECONDS)
            .build()
        try {
            val api = Retrofit.Builder().baseUrl("http://192.168.4.1/").client(client)
                .addConverterFactory(MoshiConverterFactory.create(
                    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()))
                .build().create(Esp32ApiService::class.java)
            return api.configurarWifi(request)
        } finally {
            client.connectionPool.evictAll()
            client.dispatcher.executorService.shutdown()
        }
    }
}
