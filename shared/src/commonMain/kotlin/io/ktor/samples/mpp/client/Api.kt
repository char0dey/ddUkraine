package io.ktor.samples.mpp.client

import io.ktor.client.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.cio.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
internal expect val ApplicationDispatcher: CoroutineDispatcher

@DelicateCoroutinesApi
class ApplicationApi {

    private val client = getHttpClient()

    private var job: Job? = null

    fun stop(callback: (Boolean) -> Unit){
        job?.cancel()
        callback(false)
    }

    fun about(url: String, timeout: Long, callback: (String, Boolean) -> Unit) {
        job = CoroutineScope(Dispatchers.Main).launch {
            try {
                while (true) {
                    getUrls(url).forEach {
                        for (i in 1..10) {
                            if (it.contains("http://") || it.contains("https://")) {
                                async { getIt(callback, it) }
                            } else {
                                callback("Wrong data in input Urls!", true)
                                job?.cancel()
                            }
                        }
                    }
                    delay(timeout)
                }
            } catch (e: Exception) {
                e.message?.let { callback(it, true) }
            }
        }

        job?.invokeOnCompletion { throwable ->
            if (throwable is CancellationException)
                println("Coroutine is Cancelled!")
        }
    }

    fun getUrls(urls: String): List<String> {
        return urls?.split(";")?.map { it.trim() } ?: emptyList()
    }

    private suspend fun getIt(callback: (String, Boolean) -> Unit, url: String) {
        try {
            val result: HttpResponse = client.get {
                url(url)
            }
            callback(result.toString(), true)
        } catch (e:Exception){
            e.message?.let { callback(it, true) }
        }
    }
}
