package com.jeffrwatts.openglexperiments

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class DepthAPIActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    companion object {
        const val TAG = "RawDepthActivity"
    }

    private var showDepthMap = false
    private var isDepthSupported = false
    private var arCoreSession: Session? = null

    private val buttonShowDepth: Button by lazy { findViewById(R.id.buttonShowDepth) }
    private lateinit var surfaceView: GLSurfaceView
    private lateinit var displayRotationHelper: DisplayRotationHelper

    private val backgroundRenderer = BackgroundRenderer()
    private val depthTextureHelper = DepthTextureHandler()

    private var previousTrackingState = TrackingState.STOPPED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_depth_api)
        surfaceView = findViewById(R.id.surfaceview)
        displayRotationHelper = DisplayRotationHelper(this)

        buttonShowDepth.setOnClickListener {
            if (isDepthSupported) {
                showDepthMap = !showDepthMap
                buttonShowDepth.text = if (showDepthMap) "Hide Depth" else "Show Depth"
            } else {
                showDepthMap = false
                buttonShowDepth.text = "Depth Not Supported"
            }
        }

        // Setup SurfaceView
        surfaceView.preserveEGLContextOnPause = true;
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY;
        surfaceView.setWillNotDraw(false);
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
        if (arCoreSession == null) {
            Log.d(TAG, "Create Session")
            arCoreSession = Session(this).also { session->
                val config = session.config
                isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                config.depthMode = if (isDepthSupported) Config.DepthMode.AUTOMATIC else Config.DepthMode.DISABLED
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
            depthTextureHelper.createOnGlThread()
            backgroundRenderer.createOnGlThread(this)
            backgroundRenderer.createDepthShaders(this, depthTextureHelper.depthTexture)
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
            displayRotationHelper.updateSessionIfNeeded(session)

            session.setCameraTextureName(backgroundRenderer.textureId)

            val frame = session.update()
            val camera = frame.camera

            if (isDepthSupported) {
                depthTextureHelper.update(frame)
            }

            backgroundRenderer.draw(frame)

            if (showDepthMap) {
                backgroundRenderer.drawDepth(frame)
            }
            updateScreenOnTrackingState(camera.trackingState)
            if(camera.trackingState == TrackingState.PAUSED) {
                return
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