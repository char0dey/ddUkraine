import ua.app.ddukraine.mpp.client.*
import kotlinx.browser.document

fun main() {
    ApplicationApi().about {
        val div = document.createElement("pre")
        div.textContent = it
        document.body?.appendChild(div)
    }
}
