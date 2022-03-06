package ua.app.ddukraine.mpp.client

import android.annotation.SuppressLint
import io.ktor.client.HttpClient
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import ua.app.ddukraine.mpp.client.models.Proxy
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

actual fun getHttpClient(): HttpClient {
    return HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(json)
        }
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        engine {
            config { // this: OkHttpClient.Builder ->
                followRedirects(true)
                followSslRedirects(true)
                addInterceptor(interceptor)
                connectTimeout(60, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
            }

            preconfigured = getUnsafeOkHttpClient().build()
        }

        expectSuccess = true
    }
}

fun getUnsafeOkHttpClient(): OkHttpClient.Builder = runCatching {
    OkHttpClient.Builder().apply {
        sslSocketFactory(createSslSocketFactory(), trustManager[0] as X509TrustManager)
        hostnameVerifier { hostname, session -> true }
        addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        addProxy(DI.proxyManager)
    }
}.getOrThrow()


private fun OkHttpClient.Builder.addProxy(proxyManager: ProxyManager) {
    val useProxy = proxyManager.isProxyEnabled && proxyManager.proxies.isNotEmpty()
    if (!useProxy)
        return
    val proxy: Proxy = proxyManager.proxies.random()
    proxy(java.net.Proxy(java.net.Proxy.Type.HTTP, InetSocketAddress(proxy.host, proxy.port)))
    proxyAuthenticator(Authenticator { route, response ->
        if (useProxy && response.code != 407) {
            val credentials = Credentials.basic(proxy.user, proxy.password)
            response.request.newBuilder().header("Proxy-Authorization", credentials).build()
        } else null
    })
}

private val json = Json {
    isLenient = true
    ignoreUnknownKeys = true
}

private val trustManager = arrayOf<TrustManager>(
    @SuppressLint("CustomX509TrustManager")
    object : X509TrustManager {
        @Throws(CertificateException::class)
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(
            chain: Array<X509Certificate>,
            authType: String
        ) {
        }

        @Throws(CertificateException::class)
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(
            chain: Array<X509Certificate>,
            authType: String
        ) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }
)

private fun createSslSocketFactory(): SSLSocketFactory {
    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustManager, SecureRandom())
    return sslContext.socketFactory
}