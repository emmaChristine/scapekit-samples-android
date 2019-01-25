package com.scape.scapekit

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.widget.Toast
import com.google.ar.sceneform.ux.ArFragment
import com.scape.scapekit.helper.PermissionHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

/**
 * Activity that demonstrates the use of ScapeKit.
 *
 * The Activity requests permissions required by ScapeKit in case they were not granted previously.
 *
 * In order to display the AR preview we are grabbing an ArSession with ArSession.withArFragment(sceneform_fragment).
 *
 * Upon activity resume ArSession starts tracking and we can then retrieve the current geoposition via geoSession.getCurrentGeoPosition.
 *
 */
class MainActivity : FragmentActivity() {
    val TAG = MainActivity::class.simpleName

    private var arSession: ArSession? = null
    private var geoSession: GeoSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        setupCamera()
        setupGeo()

        bindings()
    }

    /**
     * Check if permissions required by ScapeKit have been granted and prompt the user to grant the ones that haven't been granted yet.
     */
    fun checkPermissions() {
        val deniedPermissions = PermissionHelper.checkPermissions(this)
        if (!deniedPermissions.isEmpty())
            displayToast("Denied Permissions: ${Arrays.toString(deniedPermissions)}")

        PermissionHelper.requestPermissions(this, deniedPermissions)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        PermissionHelper.processResult(this, requestCode, permissions, grantResults)
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
    /**
     * Example on how to start continuous geoposition fetching.
     */
    fun startFetch() {
        geoSession?.startFetch(
                sessionStarted = {
                    Log.d(TAG, "Session started")
                },
                positionRawEstimated = {

                },
                positionLocked = {

                },
                sessionError = {
                    Log.d(TAG, "Session error ${it.errorMessage}")
                })
    }

    /**
     * Example on how to stop continuous geoposition fetching.
     */
    fun stopFetch() {
        geoSession?.stopFetch(
                sessionClosed = {
                    Log.d(TAG, "Session stopped")
                },
                sessionError = {
                    Log.d(TAG, "Session error ${it.errorMessage}")
                })
    }

    private fun setupCamera() {
        arSession = ArSessionApp.sharedInstance.scapeClient.arSession?.withArFragment(sceneform_fragment as ArFragment)
        arSession?.isDebugMode = false
        arSession?.isPlaneDetection = true
        arSession?.isLightEstimation = true
    }

    private fun setupGeo() {
        geoSession = ArSessionApp.sharedInstance.scapeClient.geoSession
    }

    private fun bindings() {
        localize_button.setOnClickListener {
            getCurrentPositionAsync()
        }
    }

    private fun getCurrentPositionAsync() {
        geoSession?.getCurrentGeoPosition(
                positionRawEstimated = { details ->
                    val coordinates = "${details.rawLocation.coordinates.latitude} ${details.rawLocation.coordinates.longitude} ${details.rawLocation.coordinates.altitude}"
                    Log.d(TAG, "Retrieving GPS LocationCoordinates: $coordinates")
                },
                positionLocked = { details ->
                    val coordinates = "${details.lockedCoordinates.latitude} ${details.lockedCoordinates.longitude}"
                    Log.d(TAG, "Retrieving Scape GeoCoordinates: $coordinates")
                },
                sessionError = { details ->
                    Log.e(TAG, "Could not retrieve geo coordinates: ${details.errorMessage}")
                })
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
