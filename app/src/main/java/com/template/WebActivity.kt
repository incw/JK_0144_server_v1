package com.template

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.webkit.*
import androidx.appcompat.app.AppCompatActivity
import com.template.databinding.ActivityWebBinding
import io.ktor.http.*

@SuppressLint("SetJavaScriptEnabled")
class WebActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences

    private var _binding: ActivityWebBinding? = null
    private val binding: ActivityWebBinding
        get() = _binding ?: throw RuntimeException("ActivityWebBinding == null")


    private lateinit var cookieManager: CookieManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityWebBinding.inflate(layoutInflater)
        webViewSettings()
        setContentView(binding.root)
        if (savedInstanceState != null) {
            binding.webView.restoreState(savedInstanceState)
        }
        preferences = getSharedPreferences("LINKS", Context.MODE_PRIVATE)


    }

    private fun webViewSettings() = with(binding) {

        supportActionBar?.hide()

        preferences = getSharedPreferences("LINKS", Context.MODE_PRIVATE)

        val defaultUserAgent = WebSettings.getDefaultUserAgent(applicationContext)
        val loadUrl = preferences.getString(FINAL_URL, null)

        webView.webViewClient = WebViewClient()

        webView.settings.userAgentString = defaultUserAgent
        webView.settings.javaScriptEnabled = true
        webView.settings.userAgentString


        cookieManager = CookieManager.getInstance()

        val cookies = cookieManager.getCookie(loadUrl)

        if (cookies != null) {
            val cookieHeader = HashMap<String, String>()
            cookieHeader["Cookie"] = cookies
            webView.loadUrl(loadUrl ?: throw RuntimeException("cannot find url"), cookieHeader)
        } else {
            webView.loadUrl(loadUrl ?: throw RuntimeException("cannot find url"))
        }

    }


    override fun onPause() {
        super.onPause()

        preferences = getSharedPreferences("LINKS", Context.MODE_PRIVATE)
        val cookies = cookieManager.getCookie(preferences.getString(FINAL_URL, null))
        val pref = preferences.edit().putString(COOKIES, cookies).apply()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onResume() {
        preferences = getSharedPreferences("LINKS", Context.MODE_PRIVATE)
        super.onResume()
        val cookies = preferences.getString(COOKIES, null)
        if (cookies != null) {
            cookieManager.setCookie(preferences.getString(FINAL_URL, null), cookies)
            cookieManager.flush()

        }
    }


    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = with(binding) {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    override fun onDestroy() {
        preferences = getSharedPreferences("LINKS", Context.MODE_PRIVATE)
        val cookies = cookieManager.getCookie(preferences.getString(FINAL_URL, null))
        val pref = preferences.edit().putString(COOKIES, cookies).apply()
        super.onDestroy()

    }

    companion object {
        const val FINAL_URL = "final_url"
        const val COOKIES = "cookies"
    }
}