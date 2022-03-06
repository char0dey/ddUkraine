package ua.app.ddukraine.mpp.client

import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import ua.app.ddukraine.mpp.client.models.Proxy
import java.time.LocalDateTime


@Suppress("EXPERIMENTAL_API_USAGE")
class MainActivity : AppCompatActivity() {
    lateinit var container: Group
    lateinit var progressBar: ProgressBar
    var api = ApplicationApi()
    var timeout = 500L
    var threadCount = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.contentGroup)
        progressBar = findViewById(R.id.progressBar)
        val useProxyCheckBox: CheckBox = findViewById(R.id.useProxyCheckBox)
        val useCustomProxyCheckBox: CheckBox = findViewById(R.id.useCustomProxy)
        val customProxyEditText: EditText = findViewById(R.id.customProxyEditText)
        val resultTv: TextView = findViewById(R.id.resultTv)
        resultTv.movementMethod = ScrollingMovementMethod()
        val startBtn: AppCompatButton = findViewById(R.id.startBtn)
        val urlEt: AppCompatEditText = findViewById(R.id.urlEt)
        urlEt.movementMethod = ScrollingMovementMethod()
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

        ArrayAdapter.createFromResource(
            this,
            R.array.long_array,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            timeSpinner.adapter = adapter
        }

        useProxyCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                loadGeneratedProxies()
            }
            useCustomProxyCheckBox.isChecked = !isChecked
        }
        useCustomProxyCheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            customProxyEditText.isVisible = isChecked
            useProxyCheckBox.isChecked = !isChecked
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
                    if (useCustomProxyCheckBox.isChecked) {
                        if (customProxyEditText.text.toString().isBlank()) {
                            Toast.makeText(this, "Empty proxy", Toast.LENGTH_LONG).show()
                        } else {
                            runCatching {
                                val proxies = customProxyEditText.text.toString().split("\n")
                                    .mapIndexed{ index, input -> Proxy.parse(input, index) }
                                DI.proxyManager.saveProxies(proxies)
                            }.onFailure {
                                Toast.makeText(this, "Invalid input", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    DI.proxyManager.isProxyEnabled = useProxyCheckBox.isChecked || useCustomProxyCheckBox.isChecked
                    startBtn.text = "Stop DDoS"
                    api = ApplicationApi()
                    api.startSending(urlEt.text.toString(), timeout, threadCount) { res, isRun ->
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

    private fun loadGeneratedProxies() {
        progressBar.isVisible = true
        container.isVisible = false
        DI.proxyManager.isProxyEnabled = false
        api = ApplicationApi()
        api.loadProxies {
            progressBar.isVisible = false
            container.isVisible = true
            it.onSuccess { proxies ->
                DI.proxyManager.saveProxies(proxies)
            }.onFailure {
                it.printStackTrace()
            }
        }
    }
}
