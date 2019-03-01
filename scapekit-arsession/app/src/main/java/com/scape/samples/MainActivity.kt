package com.scape.samples

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.widget.Toast
import com.bosphere.filelogger.FL
import com.google.ar.sceneform.ux.ArFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.scape.scapekit.*
import com.scape.scapekit.BuildConfig
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*


/**
 * Activity that demonstrates the use of ScapeKit.
 *
 * In order to display the AR preview we are grabbing an ArSession with `ArSession.withArFragment(sceneform_fragment)`.
 * On `localize_button` button press we attempt to retrieve the current geo-position(Position and Orientation) via `ScapeSession.getMeasurements`
 * with `GeoSourceType.RAWSENSORSANDSCAPEVISIONENGINE` flag to ensure a very precise localization.
 *
 */
class MainActivity : FragmentActivity(), ScapeSessionObserver, ArSessionObserver, OnMapReadyCallback {

    val TAG = MainActivity::class.java.simpleName

    private val REQUEST_OVERLAY = 11
    private var arSession: ArSession? = null
    private var scapeSession: ScapeSession? = null
    private var mapView: MapView? = null
    private lateinit var mapBox: MapboxMap
    private var lastLat = 0.0
    private var lastLong = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupMap(savedInstanceState)

        enableOverlay()

        setupCamera()
        setupGeo()

        bindings()
    }

    override fun onStart() {
        super.onStart()
    }

    fun setupMap(savedInstanceState: Bundle?) {
        val mapFragment: SupportMapFragment? =
                supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment

        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        googleMap ?: return
        with(googleMap) {
            moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, ZOOM_LEVEL))
            addMarker(MarkerOptions().position(SYDNEY))
        }
    }

    // Allow debug logs can be displayed on an overlay view
    fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${BuildConfig.APPLICATION_ID}"))
            startActivityForResult(intent, REQUEST_OVERLAY)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_OVERLAY
                // Setting screen does not have Ok button, thus result code is always RESULT_CANCELED
                && resultCode == Activity.RESULT_CANCELED
                && Settings.canDrawOverlays(this)) {
            displayToast("Permission granted, debug logs wil be displayed after the next app launch")
        } else {
            displayToast("Please grant a permission to see debug logs on overlay")
            finish()
        }

    }

    override fun onResume() {
        super.onResume()

        mapView?.onResume()
        arSession?.startTracking()
        startFetch()
    }

    override fun onPause() {
        super.onPause()

        mapView?.onPause()
        arSession?.stopTracking()
        stopFetch()
    }

    override fun onDestroy() {
        mapView?.onDestroy()
        arSession?.stopTracking()
        stopFetch()

        super.onDestroy()
    }

    override fun onScapeSessionError(p0: ScapeSession?, p1: ScapeSessionState, p2: String) {
        Log.d(TAG, "Could not retrieve geo coordinates: $p2")

        // log to file
        FL.e("\nScapeSessionError state: $p1 message: $p2 \n")
    }

    override fun onDeviceMotionMeasurementsUpdated(p0: ScapeSession?, p1: MotionMeasurements?) {
        Log.d(TAG, "onDeviceMotionMeasurementsUpdated: $p1")

        // log to file
        FL.i("\nMotionMeasurements $p1\n")
    }

    override fun onScapeMeasurementsUpdated(p0: ScapeSession?, p1: ScapeMeasurements?) {
        Log.d(TAG, "onScapeMeasurementsUpdated: $p1")

        // log to file
        FL.i("\nonScapeMeasurementsUpdated $p1\n")
    }

    override fun onDeviceLocationMeasurementsUpdated(p0: ScapeSession?, details: LocationMeasurements?) {
        Log.d(TAG, "Retrieving GPS LocationCoordinates: ${details?.coordinates}")

        lastLat = details?.coordinates?.latitude!!
        lastLong = details?.coordinates?.longitude!!

        // log to file
        FL.v("\nLocationMeasurements $details\n")

        // Update Mapbox
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
        scapeSession?.startFetch(GeoSourceType.RAWSENSORS, this)
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
