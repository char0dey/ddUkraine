import ua.app.ddukraine.mpp.client.*
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun main() = runBlocking {
    Platform.isMemoryLeakCheckerActive = false // required until https://youtrack.jetbrains.com/issue/KT-43772 is fixed

    val result = suspendCoroutine<String> { continuation ->
        ApplicationApi().startSending {
            continuation.resume(it)
        }
    }

    println("Result: $result")
}