package com.scape.samples

import android.app.Application
import android.util.Log
import com.scape.scapekit.Scape
import com.scape.scapekit.ScapeClient

/**
 * Basic Application class that demonstrates the initialisation of the ScapeClient.
 *
 * ScapeClient entry point is acquired with debugSupport and ArSupport enabled and with our own Api Key.
 */
class ArSessionApp : Application() {

    companion object {
        private const val TAG = "ScapeArSessionApp"

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
                .withApiKey("ZH8wQNckWg8Tf1VHUjn8QaTbdEgWs1mS2iMaSfb8")
                .withXrSupport(true)
                .build()
    }
}