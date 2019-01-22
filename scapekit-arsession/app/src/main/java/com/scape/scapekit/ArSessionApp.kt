package com.scape.scapekit

import android.app.Application
import android.util.Log
import com.scape.scapekit.BuildConfig
import com.scape.scapekit.Scape
import com.scape.scapekit.ScapeClient

class ArSessionApp : Application() {

    companion object {
        private const val TAG = "ArSessionApp"

        private var mSharedInstance: ArSessionApp? = null
        var sharedInstance: ArSessionApp
            get() = mSharedInstance!!
            private set(value) {
                mSharedInstance = value
            }
    }

    lateinit var scapeClient: ScapeClient

    override fun onCreate() {
        super.onCreate()

        Log.i(TAG, "onCreate: Application created")

        sharedInstance = this

        scapeClient = Scape.scapeClientBuilder
                .withContext(applicationContext)
                .withDebugSupport(true)
                .withApiKey(BuildConfig.SCAPEKIT_API_KEY)
                .withArSupport(true)
                .build()

        scapeClient.start(clientStarted = {
            Log.i(TAG, "ScapeClient started")
        }, clientFailed = {
            Log.i(TAG, it)
        })
    }
}