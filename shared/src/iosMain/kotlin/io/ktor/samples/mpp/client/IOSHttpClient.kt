package io.ktor.samples.mpp.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.ios.Ios

actual fun getHttpClient(): HttpClient {
    return HttpClient(Ios) {
        expectSuccess = true
    }
}
