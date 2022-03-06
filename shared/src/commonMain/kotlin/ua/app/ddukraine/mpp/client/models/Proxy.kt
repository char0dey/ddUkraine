package ua.app.ddukraine.mpp.client.models

import kotlinx.serialization.Serializable

@Serializable
data class Proxy(
    val id: Int,
    val ip: String,
    val auth: String
) {
    val user: String
        get() = auth.split(":").first()
    val password: String
        get() = auth.split(":").get(1)
    val host: String
        get() = ip.replaceAfter(":", "").dropLast(1)
    val port: Int
        get() = ip.split(":").last().toInt()

    companion object {
        fun parse(raw: String, index: Int): Proxy = Proxy(
            id = index,
            ip = raw.split(":")[0] + ":" + raw.split(":")[1],
            auth = raw.split(":")[2] + ":" + raw.split(":")[3]
        )
    }
}