package com.scape.samples

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.afollestad.assent.Permission.*
import com.afollestad.assent.askForPermissions
import com.google.ar.sceneform.ux.ArFragment
import com.scape.scapekit.*
import kotlinx.android.synthetic.main.scape_ar_fragment.*
import java.util.*

/**
 * Activity that demonstrates the use of ScapeKit.
 *
 * The Activity requests permissions required by ScapeKit in case they were not granted previously.
 *
 * In order to display the XR preview we are grabbing an XrSession with `XrSession.withArFragment(sceneform_fragment)`.
 * On `localize_button` button press we attempt to retrieve the current geo-position(Position and Orientation) via `ScapeSession.getMeasurements`
 * with `GeoSourceType.RAWSENSORSANDSCAPEVISIONENGINE` flag to ensure a very precise localization.
 *
 */
@SuppressLint("LogNotTimber")
class MainActivity : FragmentActivity(), ScapeSessionObserver, XrSessionObserver {

    companion object {
        private const val TAG = "ScapeMainActivity"
        private const val REQUEST_OVERLAY = 11
    }

    private var arePermissionsGranted: Boolean = false

    private var xrSession: XrSession? = null
    private var scapeSession: ScapeSession? = null
    private lateinit var scapeClient: ScapeClient

    // region Activity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        scapeClient = ArSessionApp.sharedInstance.scapeClient

        enableOverlay()

        askForPermissions(WRITE_EXTERNAL_STORAGE,
                          READ_EXTERNAL_STORAGE,
                          READ_PHONE_STATE,
                          ACCESS_FINE_LOCATION,
                          ACCESS_COARSE_LOCATION,
                          CAMERA) { result ->
            arePermissionsGranted = result.isAllGranted(WRITE_EXTERNAL_STORAGE,
                                                        READ_EXTERNAL_STORAGE,
                                                        READ_PHONE_STATE,
                                                        ACCESS_FINE_LOCATION,
                                                        ACCESS_COARSE_LOCATION,
                                                        CAMERA)
            when {
                arePermissionsGranted -> {
                    displayToast("All permissions granted")

                    val container = findViewById<View>(R.id.activity_main) as FrameLayout
                    val inflatedLayout = layoutInflater.inflate(R.layout.scape_ar_fragment, null, false)
                    container.addView(inflatedLayout)

                    scapeClient.start(clientStarted = {
                        Log.i(TAG, "ScapeClient started")

                        setupCamera()
                        setupGeo()
                        bindings()
                    }, clientFailed = {
                        Log.e(TAG, it)

                        displayToast(it)
                    })
                }
                !arePermissionsGranted ->
                    displayToast("All permissions denied, cannot start ScapeClient")
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if(arePermissionsGranted) {
            scapeClient.start(clientStarted = {
                Log.i(TAG, "ScapeClient started")
            }, clientFailed = {
                Log.i(TAG, it)
            })

            xrSession?.startTracking()
        }
    }

    override fun onPause() {
        super.onPause()

        scapeClient.stop()

        xrSession?.stopTracking()
    }

    override fun onDestroy() {
        scapeClient.terminate()

        super.onDestroy()
    }

    // endregion Activity

    // region ScapeSessionObserver

    override fun onScapeSessionError(session: ScapeSession?, p1: ScapeSessionState, p2: String) {
        Log.d(TAG, "Could not retrieve geo coordinates: $p2")
    }

    override fun onDeviceMotionMeasurementsUpdated(session: ScapeSession?, p1: MotionMeasurements?) {
        Log.d(TAG, "onDeviceMotionMeasurementsUpdated: $p1")
    }

    override fun onScapeMeasurementsUpdated(session: ScapeSession?, p1: ScapeMeasurements?) {
        Log.d(TAG, "onScapeMeasurementsUpdated: $p1")
    }

    override fun onDeviceLocationMeasurementsUpdated(session: ScapeSession?, details: LocationMeasurements?) {
        Log.d(TAG, "retrieving GPS LocationCoordinates: ${details?.coordinates}")
    }

    override fun onCameraTransformUpdated(session: ScapeSession?, p1: ArrayList<Double>?) {
    }

    // endregion Permissions

    // region XrSessionObserver

    override fun onTrackingStateUpdated(session: XrSession?, details: XrTrackingState) {
        Log.d(TAG, "onTrackingStateUpdated: $details")
    }

    override fun onPlaneDetected(session: XrSession?, details: XrPlane) {
        Log.d(TAG, "onPlaneDetected: $details")
    }

    override fun onFrameUpdated(session: XrSession?, details: XrFrame) {
        Log.d(TAG, "onFrameUpdated: $details")
    }

    override fun onPlaneUpdated(session: XrSession?, details: XrPlane) {
        Log.d(TAG, "onPlaneUpdated: $details")
    }

    override fun onPlaneRemoved(session: XrSession?, details: XrPlane) {
        Log.d(TAG, "onPlaneRemoved: $details")
    }

    // endregion Permissions

    // region Setup

    private fun setupCamera() {
        xrSession = scapeClient.xrSession?.withArFragment(fragment = sceneform_fragment as ArFragment)
        xrSession?.isDebugMode = false
        xrSession?.isPlaneDetection = true
        xrSession?.isLightEstimation = true
        xrSession?.xrSessionObserver = this
    }

    private fun setupGeo() {
        scapeSession = scapeClient.scapeSession
    }

    private fun bindings() {
        localize_button.setOnClickListener {
            getCurrentPositionAsync()
        }
    }

    // endregion Setup

    // region Geoposition

    private fun getCurrentPositionAsync() {
        assert(scapeSession == null) {
            Log.d(TAG, "scapeSession is null")
        }

        scapeSession!!.getMeasurements(GeoSourceType.RAWSENSORSANDSCAPEVISIONENGINE, this)
    }

    /**
     * Example on how to start continuous geoposition fetching using Scape Vision Engine.
     */
    private fun startFetch() {
        assert(scapeSession == null) {
            Log.d(TAG, "scapeSession is null")
        }

        scapeSession!!.startFetch(GeoSourceType.RAWSENSORSANDSCAPEVISIONENGINE, this)
    }

    /**
     * Example on how to stop continuous geoposition fetching.
     */
    private fun stopFetch() {
        assert(scapeSession == null) {
            Log.d(TAG, "scapeSession is null")
        }

        scapeSession!!.stopFetch()
    }

    // endregion Geoposition

    // region Debug

    // Allow debug logs can be displayed on an overlay view
    private fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQUEST_OVERLAY)
        }
    }

    /**
     * Display long toast  to notify the user of progress.
     *
     * @param message The toast message.
     */
    private fun displayToast(message: String) {
        runOnUiThread { Toast.makeText(applicationContext, "$TAG:$message", Toast.LENGTH_LONG).show() }
    }

    // endregion Debug overlay

}
