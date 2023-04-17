package com.template

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.template.databinding.ActivityWebBinding

@SuppressLint("SetJavaScriptEnabled")
class WebActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences
    val historyList: ArrayList<String> = arrayListOf()
    private var _false = false
    private var _binding: ActivityWebBinding? = null
    private val binding: ActivityWebBinding
        get() = _binding ?: throw RuntimeException("ActivityWebBinding == null")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityWebBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        binding.webView.settings.javaScriptEnabled = true
        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
        }
        webViewSettings()
    }

    private fun webViewSettings() = with(binding) {

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        supportActionBar?.hide()
        preferences = getSharedPreferences("LINKS", Context.MODE_PRIVATE)
        val defaultUserAgent = WebSettings.getDefaultUserAgent(baseContext)

        val loadUrl = preferences.getString(FINAL_URL, null)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {


            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                view?.settings?.userAgentString = defaultUserAgent
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                CookieManager.getInstance().flush()
                if (url != null) {
                    historyList.add(url)
                }
                Log.d("list", historyList.toString())
            }
        }
        WebView.setWebContentsDebuggingEnabled(true)

        webView.loadUrl(loadUrl ?: throw RuntimeException("loadUrl was null"))

    }

    private fun lastItem(): String {
        _false = true
        val lastItem = historyList.last()
        if (historyList.size > 1) {
            historyList.clear()
            historyList.add(lastItem)
        }
        return lastItem
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }
    override fun onStop() {
        super.onStop()
        CookieManager.getInstance().flush()
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!_false) {
            lastItem()
            _false = true
        }
        if (historyList.size > 1) {
            historyList.removeAt(historyList.lastIndex)
            binding.webView.goBack()
        }

    }
    companion object {
        const val FINAL_URL = "final_url"
    }
}