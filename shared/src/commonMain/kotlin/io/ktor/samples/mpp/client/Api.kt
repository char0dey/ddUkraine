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

                    for (i in 1..10) {
                        async {  getIt(callback, url) }
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
