package com.scape.scapekit

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.widget.Toast
import com.google.ar.sceneform.ux.ArFragment
import com.scape.scapekit.utils.CoordinatesUtils
import kotlinx.android.synthetic.main.activity_main.*
import java.util.ArrayList

/**
 * Activity that demonstrates the use of ScapeKit.
 *
 * In order to display the AR preview we are grabbing an ArSession with `ArSession.withArFragment(sceneform_fragment)`.
 * On `localize_button` button press we attempt to retrieve the current geo-position(Position and Orientation) via `ScapeSession.getMeasurements`
 * with `GeoSourceType.RAWSENSORSANDSCAPEVISIONENGINE` flag to ensure a very precise localization.
 *
 */
class MainActivity : FragmentActivity(), ScapeSessionObserver, ArSessionObserver {

    val TAG = MainActivity::class.java.simpleName

    private var arSession: ArSession? = null
    private var scapeSession: ScapeSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupCamera()
        setupGeo()

        bindings()
    }

    override fun onResume() {
        super.onResume()

        arSession?.startTracking()
    }

    override fun onPause() {
        super.onPause()

        arSession?.stopTracking()
    }

    override fun onDestroy() {
        arSession?.stopTracking()

        super.onDestroy()
    }

    override fun onScapeSessionError(p0: ScapeSession?, p1: ScapeSessionState, p2: String) {
        Log.d(TAG, "Could not retrieve geo coordinates: $p2")
    }

    override fun onDeviceMotionMeasurementsUpdated(p0: ScapeSession?, p1: MotionMeasurements?) {
        Log.d(TAG, "onDeviceMotionMeasurementsUpdated: $p1")
    }

    override fun onScapeMeasurementsUpdated(p0: ScapeSession?, p1: ScapeMeasurements?) {
        Log.d(TAG, "onScapeMeasurementsUpdated: $p1")
    }

    override fun onDeviceLocationMeasurementsUpdated(p0: ScapeSession?, details: LocationMeasurements?) {
        Log.d(TAG, "Retrieving GPS LocationCoordinates: ${details?.coordinates}")
    }

    override fun onCameraTransformUpdated(p0: ScapeSession?, p1: ArrayList<Double>?) {
    }

    override fun onTrackingStateUpdated(p0: ArSession?, details: ArTrackingState) {
        Log.d(TAG, "onTrackingStateUpdated: $details")
    }

    override fun onPlaneDetected(p0: ArSession?, details: ArPlane) {
        Log.d(TAG, "onPlaneDetected: $details")
    }

    override fun onFrameUpdated(p0: ArSession?, details: ArFrame) {
        Log.d(TAG, "onFrameUpdated: $details")
    }

    override fun onPlaneUpdated(p0: ArSession?, details: ArPlane) {
        Log.d(TAG, "onPlaneUpdated: $details")
    }

    override fun onPlaneRemoved(p0: ArSession?, details: ArPlane) {
        Log.d(TAG, "onPlaneRemoved: $details")
    }

    private fun setupCamera() {
        arSession = ArSessionApp.sharedInstance.scapeClient.arSession?.withArFragment(sceneform_fragment as ArFragment)
        arSession?.isDebugMode = false
        arSession?.isPlaneDetection = true
        arSession?.isLightEstimation = true
        arSession?.arSessionObserver = this
    }

    private fun setupGeo() {
        scapeSession = ArSessionApp.sharedInstance.scapeClient.scapeSession
    }

    private fun bindings() {
        localize_button.setOnClickListener {
            getCurrentPositionAsync()
        }
    }

    private fun getCurrentPositionAsync() {
        scapeSession?.getMeasurements(GeoSourceType.RAWSENSORSANDSCAPEVISIONENGINE, this)
    }

    /**
     * Example on how to start continuous geoposition fetching using Scape Vision Engine.
     */
    private fun startFetch() {
        scapeSession?.startFetch(GeoSourceType.RAWSENSORSANDSCAPEVISIONENGINE, this)
    }

    /**
     * Example on how to stop continuous geoposition fetching.
     */
    private fun stopFetch() {
        scapeSession?.stopFetch()
    }

    /**
     * Display long toast  to notify the user of progress.
     *
     * @param message The toast message.
     */
    private fun displayToast(message: String) {
        runOnUiThread { Toast.makeText(applicationContext, "$TAG:$message", Toast.LENGTH_LONG).show() }
    }

}
