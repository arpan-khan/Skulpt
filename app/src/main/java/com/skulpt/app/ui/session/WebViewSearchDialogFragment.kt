package com.skulpt.app.ui.session

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.skulpt.app.R
import com.skulpt.app.data.model.AppSettings

class WebViewSearchDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "WebViewSearchDialog"
        const val REQUEST_KEY = "webview_search_request"
        const val RESULT_IMAGE_URL = "result_image_url"
        private const val ARG_QUERY = "arg_query"
        private const val ARG_BASE_QUERY = "arg_base_query"
        private const val ARG_HW_ACCEL = "arg_hw_accel"
        private const val ARG_UA = "arg_ua"

        fun newInstance(query: String, baseQuery: String?, hwAccel: Boolean, ua: String): WebViewSearchDialogFragment {
            return WebViewSearchDialogFragment().apply {
                arguments = bundleOf(
                    ARG_QUERY to query,
                    ARG_BASE_QUERY to baseQuery,
                    ARG_HW_ACCEL to hwAccel,
                    ARG_UA to ua
                )
            }
        }
    }

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var scrollView: android.widget.ScrollView

    private var currentSettings = AppSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Skulpt_FullScreenDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_image_picker_webview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        webView = view.findViewById(R.id.webview_picker)
        progressBar = view.findViewById(R.id.progress_webview)
        statusText = view.findViewById(R.id.tv_webview_status)
        scrollView = view.findViewById(R.id.scroll_webview_status)

        val btnGoogle = view.findViewById<android.widget.Button>(R.id.btn_search_google)
        val btnDDG = view.findViewById<android.widget.Button>(R.id.btn_search_ddg)
        val btnBing = view.findViewById<android.widget.Button>(R.id.btn_search_bing)
        val btnReload = view.findViewById<android.widget.ImageButton>(R.id.btn_webview_reload)
        val btnExternal = view.findViewById<android.widget.ImageButton>(R.id.btn_open_external)

        setupWebView()

        btnGoogle.setOnClickListener { loadSearch("Google") }
        btnDDG.setOnClickListener { loadSearch("DDG") }
        btnBing.setOnClickListener { loadSearch("Bing") }
        btnReload.setOnClickListener {
            updateStatus("Reloading...")
            webView.reload()
        }
        btnExternal.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(webView.url ?: "https://duckduckgo.com"))
            startActivity(intent)
        }

        val initialQuery = arguments?.getString(ARG_QUERY) ?: ""
        loadSearch("Google", initialQuery)
    }

    private fun setupWebView() {

        webView.setBackgroundColor(Color.WHITE)

        val hwAccel = arguments?.getBoolean(ARG_HW_ACCEL, true) ?: true
        if (!hwAccel) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            val ua = arguments?.getString(ARG_UA) ?: ""
            userAgentString = if (ua.isNotEmpty()) ua
                else "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress == 100) View.GONE else View.VISIBLE
            }
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let { Log.d(TAG, "JS: ${it.message()}") }
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                view?.loadUrl(request?.url?.toString() ?: return false)
                return true
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                updateStatus("Loading: ${url?.takeLast(30)}")
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                updateStatus("Finished. Long-press image to select.")
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                updateStatus("Error: ${error?.toString()}")
            }
        }

        webView.setOnLongClickListener {
            val result = webView.hitTestResult
            val imageUrl = result.extra
            if (result.type == WebView.HitTestResult.IMAGE_TYPE || result.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                if (!imageUrl.isNullOrEmpty()) {
                    setFragmentResult(REQUEST_KEY, bundleOf(RESULT_IMAGE_URL to imageUrl))
                    dismiss()
                    true
                } else false
            } else false
        }
    }

    private fun loadSearch(provider: String, queryOverride: String? = null) {
        val baseQueryArg = queryOverride ?: arguments?.getString(ARG_QUERY) ?: ""
        val userBaseQuery = arguments?.getString(ARG_BASE_QUERY)
        val suffix = if (userBaseQuery.isNullOrBlank()) "workout" else userBaseQuery.trim()

        val query = baseQueryArg.replace(" ", "+")
        val suffixEncoded = suffix.replace(" ", "+")

        val url = when (provider) {
            "Google" -> "https://www.google.com/search?q=$query+$suffixEncoded&tbm=isch"
            "DDG" -> "https://duckduckgo.com/?q=$query+$suffixEncoded&iax=images&ia=images"
            "Bing" -> "https://www.bing.com/images/search?q=$query+$suffixEncoded"
            else -> ""
        }
        if (url.isNotEmpty()) {
            updateStatus("Searching $provider...")
            webView.loadUrl(url)
        }
    }

    private fun updateStatus(msg: String) {
        val newText = if (statusText.text.isEmpty()) msg else "${statusText.text}\n$msg"
        statusText.text = newText
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onDestroyView() {
        webView.stopLoading()
        webView.destroy()
        super.onDestroyView()
    }
}
