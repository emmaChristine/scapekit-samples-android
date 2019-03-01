package com.scape.samples

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.google.ar.core.Anchor
import com.google.ar.core.Config
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import com.scape.pixscape.R
import com.scape.scapekit.*
import com.scape.scapekit.helper.ARCoreAvailabilityHelper

import java.util.ArrayList


/**
 * Example activity that already uses Sceneform UX and then integrates with ScapeKit.
 */
class SceneformActivity : AppCompatActivity(), ScapeSessionObserver, ArSessionObserver {

    val TAG = SceneformActivity::class.java.simpleName

    private val REQUEST_OVERLAY = 11
    lateinit var arFragment: ArFragment
    private var arSession: ArSession? = null
    private var scapeSession: ScapeSession? = null

    private var session: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sceneform)

        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment

        enableOverlay()

        ARCoreAvailabilityHelper.requestInstallIfRequired(this)

        initModel()

        bindings()
    }

    override fun onResume() {
        super.onResume()

        arSession?.startTracking()
        startFetch()
    }

    override fun onPause() {
        super.onPause()

        arSession?.stopTracking()
        stopFetch()
    }

    override fun onDestroy() {
        arSession?.stopTracking()

        super.onDestroy()
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

    fun bindings() {
        arSession = ArSessionApp.sharedInstance.scapeClient.arSession?.withArFragment(arFragment)

        arSession?.isDebugMode = false
        arSession?.isPlaneDetection = true
        arSession?.isLightEstimation = true
        arSession?.arSessionObserver = this

        scapeSession = ArSessionApp.sharedInstance.scapeClient.scapeSession

        session = Session(this)
        println("CONFIG:: " + session!!.config.toString())  //  S L 4.0.10 AArch641

        // IMPORTANT!!!  ArSceneView requires the `LATEST_CAMERA_IMAGE` non-blocking update mode.
        val config = Config(session)
        config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
        session!!.configure(config)

        contentScene

//        scapeSession.setARSession
    }

    fun initModel() {
        var earth_image = findViewById<ImageView>(R.id.earth_image)
        earth_image.setImageResource(R.drawable.droid_thumb)

        earth_image.setOnClickListener {

                view -> addObject(Uri.parse("NOVELO_EARTH.sfb"))
                scapeSession?.getMeasurements(GeoSourceType.RAWSENSORSANDSCAPEVISIONENGINE, this)
        }
    }

    fun addObject(model: Uri) {
        val frame = arFragment.arSceneView.arFrame

        val pt = getScreenCenter()
        val hits: List<HitResult>
        if (frame != null) {
            hits = frame!!.hitTest(pt.x.toFloat(), pt.y.toFloat())
            for (hit in hits) {
                val trackable = hit.trackable
                if (trackable is Plane && (trackable as Plane).isPoseInPolygon(hit.hitPose)) {
                    placeObject(arFragment, hit.createAnchor(), model)
                    break
                }
            }
        }
    }

    private fun placeObject(fragment: ArFragment, anchor: Anchor, model: Uri) {
        val renderableFuture = ModelRenderable.builder()
            .setSource(fragment.context!!, model)
            .build()
            .thenAccept { renderable -> addNodeToScene(fragment, anchor, renderable) }
            .exceptionally { throwable ->
                val builder = AlertDialog.Builder(this)
                builder.setMessage(throwable.message)
                    .setTitle("Codelab error!")
                val dialog = builder.create()
                dialog.show()
                null
            }
    }

    private fun addNodeToScene(fragment: ArFragment, anchor: Anchor, renderable: Renderable) {
        val anchorNode = AnchorNode(anchor)
        val node = TransformableNode(fragment.transformationSystem)
        node.renderable = renderable
        node.setParent(anchorNode)
        fragment.arSceneView.scene.addChild(anchorNode)
        node.select()
    }

    private fun getScreenCenter(): android.graphics.Point {
        val vw = findViewById<View>(android.R.id.content)
        return android.graphics.Point(vw.getWidth() / 2, vw.getHeight() / 2)
    }

    override fun onScapeSessionError(p0: ScapeSession?, p1: ScapeSessionState, p2: String) {
        Log.d(TAG, "Could not retrieve geo coordinates: $p2")
    }

    override fun onDeviceMotionMeasurementsUpdated(p0: ScapeSession?, p1: MotionMeasurements?) {
        Log.d(TAG, "onDeviceMotionMeasurementsUpdated: $p1")
    }

    override fun onScapeMeasurementsUpdated(p0: ScapeSession?, p1: ScapeMeasurements?) {
        Log.d(TAG, "onScapeMeasurementsUpdated: $p1")


        //Logbot.send("onScapeMeasurementsUpdated:\n %s", p1.toString())
    }

    override fun onDeviceLocationMeasurementsUpdated(p0: ScapeSession?, details: LocationMeasurements?) {
        Log.d(TAG, "Retrieving GPS LocationCoordinates: ${details?.coordinates}")

        //Logbot.send("onDeviceLocationMeasurementsUpdated:\n %s", details)
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

    /**
     * Display long toast  to notify the user of progress.
     *
     * @param message The toast message.
     */
    private fun displayToast(message: String) {
        runOnUiThread { Toast.makeText(applicationContext, "$TAG:$message", Toast.LENGTH_LONG).show() }
    }
}
