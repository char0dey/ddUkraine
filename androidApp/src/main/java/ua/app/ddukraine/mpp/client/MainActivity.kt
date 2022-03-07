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

    var proxyType = ProxyType.noProxy

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.contentGroup)
        progressBar = findViewById(R.id.progressBar)
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

        val useNoProxyRb = findViewById<View>(R.id.useNoProxyRb) as RadioButton
        val useProxyRb = findViewById<View>(R.id.useProxyRb) as RadioButton
        val useCustomProxyRb = findViewById<View>(R.id.useCustomProxyRb) as RadioButton
        val selectProxyRg = findViewById<View>(R.id.selectProxyRg) as RadioGroup


        val radioButtonClickListener =
            View.OnClickListener { v ->
                val rb = v as RadioButton
                when (rb.id) {
                    R.id.useProxyRb -> {
                        proxyType = ProxyType.autoProxy
                        customProxyEditText.isVisible = false
                        loadGeneratedProxies()
                    }

                    R.id.useCustomProxyRb -> {
                        proxyType = ProxyType.customProxy
                        customProxyEditText.isVisible = true
                    }

                    R.id.useNoProxyRb -> {
                        proxyType = ProxyType.noProxy
                        customProxyEditText.isVisible = false
                    }
                    else -> {
                        proxyType = ProxyType.noProxy
                    }
                }
            }

        useNoProxyRb.setOnClickListener(radioButtonClickListener)
        useCustomProxyRb.setOnClickListener(radioButtonClickListener)
        useProxyRb.setOnClickListener(radioButtonClickListener)

        var isRunning: Boolean = false

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
                    if (proxyType == ProxyType.customProxy) {
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
                    DI.proxyManager.isProxyEnabled = ((proxyType == ProxyType.autoProxy) || (proxyType == ProxyType.customProxy))
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

    enum class ProxyType {
        autoProxy, customProxy, noProxy
    }
}
