package io.ktor.samples.mpp.client

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
internal expect val ApplicationDispatcher: CoroutineDispatcher

@DelicateCoroutinesApi
class ApplicationApi {
    private val client = HttpClient()

    private var job: Job? = null

    fun stop(callback: (Boolean) -> Unit){
        job?.cancel()
        callback(false)
    }

    fun about(url: String, timeout: Long, callback: (String, Boolean) -> Unit) {
        job = CoroutineScope(Dispatchers.Main).launch {
            try {
                while (true) {
                    val result: HttpResponse = client.get {
                        url(url)
                    }
                    callback(result.toString(), true)
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
}
