package ua.app.ddukraine.mpp.client

import ua.app.ddukraine.mpp.client.models.Proxy

class ProxyManager {
    private var _proxies = mutableListOf<Proxy>()

    var isProxyEnabled: Boolean = false
    val proxies: List<Proxy>
        get() = _proxies

    fun saveProxies(proxies: List<Proxy>) {
        _proxies = proxies.toMutableList()
    }
}