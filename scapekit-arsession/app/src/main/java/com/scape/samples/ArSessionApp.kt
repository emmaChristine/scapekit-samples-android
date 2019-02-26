package com.scape.samples

import android.app.Application
import android.os.Environment
import android.util.Log
import com.bosphere.filelogger.FL
import com.bosphere.filelogger.FLConfig
import com.bosphere.filelogger.FLConst
import com.scape.scapekit.Scape
import com.scape.scapekit.ScapeClient
import java.io.File


/**
 * Basic Application class that demonstrates the initialisation of the ScapeClient.
 *
 * ScapeClient entry point is acquired with debugSupport and ArSupport enabled and with our own Api Key.
 */
class ArSessionApp : Application() {

    companion object {
        val TAG = ArSessionApp::class.java.simpleName

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
        initFileLogger()

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

    fun initFileLogger() {
        FL.init(FLConfig.Builder(this)
                .minLevel(FLConst.Level.V)
                .logToFile(true)
                .dir(File(Environment.getExternalStorageDirectory(), "file_logger_demo"))
                .retentionPolicy(FLConst.DEFAULT_MAX_FILE_COUNT)
                .build())
        FL.setEnabled(true)
    }
}