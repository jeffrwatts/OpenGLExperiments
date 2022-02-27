package com.jeffrwatts.openglexperiments

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.google.ar.core.Config
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class RawDepthAPIActivity : AppCompatActivity(), GLSurfaceView.Renderer{
    companion object {
        const val TAG = "RawDepthAPIActivity"
    }

    private val textViewConfidence: TextView by lazy { findViewById(R.id.textViewConfidence) }
    private val seekBarConfidence: SeekBar by lazy { findViewById(R.id.seekBarConfidence) }
    private val textViewMaxDistance: TextView by lazy { findViewById(R.id.textViewMaxDistance) }
    private val seekBarMaxDistance: SeekBar by lazy { findViewById(R.id.seekBarMaxDistance) }
    private val switchEnablePlaneFiltering: SwitchCompat by lazy { findViewById(R.id.switchEnablePlaneFiltering) }
    private val textViewPlaneFilterDistance: TextView by lazy { findViewById(R.id.textViewPlaneFilterDistance) }
    private val seekBarPlaneFilterDistance: SeekBar by lazy { findViewById(R.id.seekBarPlaneFilterDistance) }
    private val switchEnableClustering: SwitchCompat by lazy { findViewById(R.id.switchEnableClustering) }

    private var isRawDepthSupported = false
    private var arCoreSession: Session? = null
    private var enablePlaneFiltering = false
    private var enableClustering = false

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var displayRotationHelper: DisplayRotationHelper

    private val backgroundRenderer = BackgroundRenderer()
    private val depthRenderer = DepthRenderer()
    private val boxRenderer = BoxRenderer()

    private var previousTrackingState = TrackingState.STOPPED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_raw_depth_api)
        surfaceView = findViewById(R.id.surfaceview)
        displayRotationHelper = DisplayRotationHelper(this)

        seekBarConfidence.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setConfidence(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        setConfidence(seekBarConfidence.progress)

        seekBarMaxDistance.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setMaxDistance(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        setMaxDistance(seekBarMaxDistance.progress)

        switchEnablePlaneFiltering.isChecked = enablePlaneFiltering
        switchEnablePlaneFiltering.setOnCheckedChangeListener { _, isChecked ->
            enablePlaneFiltering = isChecked
        }

        seekBarPlaneFilterDistance.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setPlaneFilterDistance(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        setPlaneFilterDistance(seekBarPlaneFilterDistance.progress)

        switchEnableClustering.isChecked = enableClustering
        switchEnableClustering.setOnCheckedChangeListener { _, isChecked ->
            enableClustering = isChecked
        }

        // Setup SurfaceView
        surfaceView.preserveEGLContextOnPause = true;
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY;
        surfaceView.setWillNotDraw(false);
    }

    private fun setConfidence(progress: Int) {
        // Progress represents a percentage.
        textViewConfidence.text = "Confidence: ${progress*10} %"
        DepthData.confidenceFilter = progress.toFloat() / 10.0
    }

    private fun setMaxDistance(progress: Int) {
        // Progress represents 0.5 meters, with 10 being infinite.
        var maxDistance = 100.0
        if (progress < 10) {
            maxDistance = progress*0.5
            textViewMaxDistance.text = "Max Distance: ${maxDistance} meters"
        } else {
            textViewMaxDistance.text = "Max Distance: No limit"
        }
        DepthData.distanceFilter = maxDistance
    }

    private fun setPlaneFilterDistance(progress: Int) {
        val planeFilterDistance = progress * 0.01
        DepthData.planeFilterDistance = planeFilterDistance
        textViewPlaneFilterDistance.text = "Plane Filter Distance: $planeFilterDistance"
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        if (arCoreSession == null) {
            Log.d(TAG, "Create Session")
            arCoreSession = Session(this).also { session->
                val config = session.config
                isRawDepthSupported = session.isDepthModeSupported(Config.DepthMode.RAW_DEPTH_ONLY)
                config.depthMode = if (isRawDepthSupported) Config.DepthMode.RAW_DEPTH_ONLY else Config.DepthMode.DISABLED
                config.focusMode = Config.FocusMode.AUTO
                session.configure(config)
            }
        }

        arCoreSession?.resume()
        surfaceView.onResume()
        displayRotationHelper.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call session.update() and get a SessionPausedException.
        displayRotationHelper.onPause()
        surfaceView.onPause()
        arCoreSession?.pause()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        try {
            backgroundRenderer.createOnGlThread(this)
            depthRenderer.createOnGlThread(this)
            boxRenderer.createOnGlThread(this)

        } catch (e: Exception) {
            Log.e(TAG, "Background Renderer threw exception", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged")
        displayRotationHelper.onSurfaceChanaged(width, height)
        GLES20.glViewport(0, 0, width, height);
    }

    override fun onDrawFrame(gl: GL10?) {
        //Log.d(TAG, "onDrawFrame")
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        arCoreSession?.let { session ->
            try {
                displayRotationHelper.updateSessionIfNeeded(session)

                session.setCameraTextureName(backgroundRenderer.textureId)

                val frame = session.update()
                val camera = frame.camera

                backgroundRenderer.draw(frame)

                val points = DepthData.create(frame, session.createAnchor(camera.pose))
                points?.let {
                    if (enablePlaneFiltering) {
                        DepthData.filterUsingPlanes(it, session.getAllTrackables(Plane::class.java))
                    }

                    depthRenderer.update(it)
                    depthRenderer.draw(camera)

                    // Draw boxes around clusters of points.
                    if (enableClustering) {
                        val clusteringHelper = PointClusteringHelper(it)
                        val clusters = clusteringHelper.findClusters()
                        clusters.forEach { aabb ->
                            boxRenderer.draw(aabb, camera)
                        }
                    }
                }

                updateScreenOnTrackingState(camera.trackingState)

            } catch (e: Exception) {
                Log.e(TAG, "Exception Thrown in onDraw", e)
            }
        }
    }
    private fun updateScreenOnTrackingState(trackingState: TrackingState) {
        if (trackingState == previousTrackingState) {
            return
        }
        Log.d(TAG, "Camera Tracking State: $trackingState")
        previousTrackingState = trackingState
        when(trackingState) {
            TrackingState.STOPPED->{
                runOnUiThread { window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
            TrackingState.TRACKING->{
                runOnUiThread { window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            }
            else->{}
        }
    }
}