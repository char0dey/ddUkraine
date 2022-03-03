package io.ktor.samples.mpp.client

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import java.time.LocalDateTime


@Suppress("EXPERIMENTAL_API_USAGE")
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val resultTv: TextView = findViewById(R.id.resultTv)
        val startBtn: AppCompatButton = findViewById(R.id.startBtn)
        val urlEt: AppCompatEditText = findViewById(R.id.urlEt)

        var isRunning: Boolean = false

        val api = ApplicationApi()

        startBtn.setOnClickListener {

            if (isRunning){
                startBtn.text = "Start DDoS"
                api.stop {
                    isRunning = it
                }
            } else {
                if (!urlEt.text.isNullOrEmpty()) {
                    startBtn.text = "Stop DDoS"
                    api.about(urlEt.text.toString()) { res, isRun ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            resultTv.text = "${LocalDateTime.now()} >>> $res"
                        } else {
                            resultTv.text = "${System.currentTimeMillis()} >>> $res"
                        }
                        isRunning = isRun
                    }
                }
            }
        }
    }
}
