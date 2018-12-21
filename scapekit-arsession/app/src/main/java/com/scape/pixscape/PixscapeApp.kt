package com.scape.pixscape

import android.app.Application
import android.util.Log
import com.scape.scapekit.Scape
import com.scape.scapekit.ScapeClient

class PixscapeApp : Application() {

    companion object {
        private const val TAG = "PixscapeApp"

        private var mSharedInstance: PixscapeApp? = null
        var sharedInstance: PixscapeApp
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