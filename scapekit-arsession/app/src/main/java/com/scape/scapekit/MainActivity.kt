package com.scape.scapekit

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.util.Log
import com.google.ar.sceneform.ux.ArFragment
import com.scape.scapekit.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : FragmentActivity() {
    private var arSession: ArSession? = null
    private var geoSession: GeoSession? = null

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
                    Log.d("PXMainActivity", "Retrieving GPS LocationCoordinates: $coordinates")
                },
                positionLocked = { details ->
                    val coordinates = "${details.lockedCoordinates.latitude} ${details.lockedCoordinates.longitude}"
                    Log.d("PXMainActivity", "Retrieving Scape GeoCoordinates: $coordinates")
                },
                sessionError = { details ->
                    Log.e("PXMainActivity", "Could not retrieve geo coordinates: ${details.errorMessage}")
                })
    }

    private fun startFetch() {
        geoSession?.startFetch(
                sessionStarted = {
                    Log.d("PXMainActivity", "Session started")
                },
                positionRawEstimated = {

                },
                positionLocked = {

                },
                sessionError = {
                    Log.d("PXMainActivity", "Session error ${it.errorMessage}")
                })
    }

    private fun stopFetch() {
        geoSession?.stopFetch(
                sessionClosed = {
                    Log.d("PXMainActivity", "Session stopped")
                },
                sessionError = {
                    Log.d("PXMainActivity", "Session error ${it.errorMessage}")
                })
    }
}
