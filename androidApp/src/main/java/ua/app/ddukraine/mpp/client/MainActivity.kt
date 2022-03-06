package ua.app.ddukraine.mpp.client

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import java.time.LocalDateTime


@Suppress("EXPERIMENTAL_API_USAGE")
class MainActivity : AppCompatActivity() {

    var timeout = 500L
    var threadCount = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val resultTv: TextView = findViewById(R.id.resultTv)
        val startBtn: AppCompatButton = findViewById(R.id.startBtn)
        val urlEt: AppCompatEditText = findViewById(R.id.urlEt)
        val timeSpinner: Spinner = findViewById(R.id.timeSpinner)
        val threadSpinner: Spinner = findViewById(R.id.threadSpinner)

        timeSpinner.onItemSelectedListener = object :  AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                timeout = parent.getItemAtPosition(pos).toString().toLong()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                timeout = 500L
            }
        }

        threadSpinner.onItemSelectedListener = object :  AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                threadCount = parent.getItemAtPosition(pos).toString().toInt()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                threadCount = 10
            }
        }

        var isRunning: Boolean = false

        val api = ApplicationApi()

        ArrayAdapter.createFromResource(
            this,
            R.array.long_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            timeSpinner.adapter = adapter
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.thread_count,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            threadSpinner.adapter = adapter
        }

        startBtn.setOnClickListener {

            if (isRunning){
                startBtn.text = "Start DDoS"
                api.stop {
                    isRunning = it
                }
            } else {
                if (!urlEt.text.isNullOrEmpty()) {
                    startBtn.text = "Stop DDoS"
                    api.about(urlEt.text.toString(), timeout, threadCount) { res, isRun ->
                        when {
                            res.contains("Wrong data") -> {
                                urlEt.error = "Wrong data in input Urs! need: http:// or https://"
                                startBtn.text = "Stop DDoS"

                            }
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                                resultTv.text = "${LocalDateTime.now()} >>> $res"
                            }
                            else -> {
                                resultTv.text = "${System.currentTimeMillis()} >>> $res"
                            }
                        }
                        isRunning = isRun
                    }
                }
            }
        }
    }
}
