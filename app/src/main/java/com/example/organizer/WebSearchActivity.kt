package com.example.organizer

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WebSearchActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnBack: Button
    private lateinit var tvTitle: TextView
    private var isFirstPage = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_search)

        initViews()
        setupWebView()
        setupClickListeners()
    }

    private fun initViews() {
        webView = findViewById(R.id.webView)
        btnBack = findViewById(R.id.btn_back)
        tvTitle = findViewById(R.id.tv_title)

        val query = intent.getStringExtra("QUERY") ?: "Google"
        tvTitle.text = "Buscando: $query"
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val query = intent.getStringExtra("QUERY") ?: "Google"
        val searchUrl = "https://www.google.com/search?q=${android.net.Uri.encode(query)}"

        webView.apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let {
                        view?.loadUrl(it)
                        // Cuando se carga una nueva URL, ya no es la primera página
                        isFirstPage = false
                    }
                    return true
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    val pageTitle = view?.title ?: "Búsqueda"
                    tvTitle.text = pageTitle

                    // Si es la página de búsqueda inicial, marcarla como primera página
                    if (url?.contains("google.com/search") == true) {
                        isFirstPage = true
                    } else {
                        isFirstPage = false
                    }
                }
            }

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
            settings.displayZoomControls = false

            loadUrl(searchUrl)
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener {
            handleBackAction()
        }
    }

    private fun handleBackAction() {
        if (webView.canGoBack() && !isFirstPage) {
            // Si puede ir atrás y NO está en la primera página, ir atrás en el historial
            webView.goBack()

            // Si después de ir atrás está en la página de búsqueda, marcarla como primera página
            val currentUrl = webView.url
            if (currentUrl?.contains("google.com/search") == true) {
                isFirstPage = true
            }
        } else {
            // Si está en la primera página o no puede ir atrás, cerrar la actividad
            finish()
        }
    }

    override fun onBackPressed() {
        handleBackAction()
    }

    // Limpiar recursos cuando se destruya la actividad
    override fun onDestroy() {
        super.onDestroy()
        webView.stopLoading()
        webView.destroy()
    }
}