package ua.app.ddukraine.mpp.client

import io.ktor.client.HttpClient

interface NetworkClient {
    suspend fun <T> execute(clientBlock: suspend HttpClient.() -> T): Result<T>
}
