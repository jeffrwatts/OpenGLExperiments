package com.jeffrwatts.openglexperiments

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.jeffrwatts.openglexperiments.helpers.DisplayRotationHelper
import com.jeffrwatts.openglexperiments.renderers.BackgroundRenderer
import com.jeffrwatts.openglexperiments.renderers.DepthTextureHandler
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MeasureDistanceActivity : AppCompatActivity(), GLSurfaceView.Renderer {

    companion object {
        const val TAG = "MeasureDistanceActivity"
    }

    private lateinit var surfaceView: GLSurfaceView
    private lateinit var displayRotationHelper: DisplayRotationHelper
    private var arCoreSession: Session? = null

    private val backgroundRenderer = BackgroundRenderer()
    private val depthTextureHelper = DepthTextureHandler()

    private var previousTrackingState = TrackingState.STOPPED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_measure_depth)

        surfaceView = findViewById(R.id.surfaceview)
        displayRotationHelper = DisplayRotationHelper(this)

        // Setup SurfaceView
        surfaceView.preserveEGLContextOnPause = true;
        surfaceView.setEGLContextClientVersion(2);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // Alpha used for plane blending.
        surfaceView.setRenderer(this);
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY;
        surfaceView.setWillNotDraw(false);
    }

    override fun onResume() {
        Log.d(DepthAPIActivity.TAG, "onResume")
        super.onResume()
        if (arCoreSession == null) {
            Log.d(DepthAPIActivity.TAG, "Create Session")
            arCoreSession = Session(this).also { session->
                val config = session.config
                session.configure(config)
            }
        }

        arCoreSession?.resume()
        surfaceView.onResume()
        displayRotationHelper.onResume()
    }

    override fun onPause() {
        Log.d(DepthAPIActivity.TAG, "onPause")
        super.onPause()
        // Note that the order matters - GLSurfaceView is paused first so that it does not try
        // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
        // still call session.update() and get a SessionPausedException.
        displayRotationHelper.onPause()
        surfaceView.onPause()
        arCoreSession?.pause()
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(DepthAPIActivity.TAG, "onSurfaceCreated")
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        try {
            depthTextureHelper.createOnGlThread()
            backgroundRenderer.createOnGlThread(this)
            backgroundRenderer.createDepthShaders(this, depthTextureHelper.depthTexture)
        } catch (e: Exception) {
            Log.e(DepthAPIActivity.TAG, "Background Renderer threw exception", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(DepthAPIActivity.TAG, "onSurfaceChanged")
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

            backgroundRenderer.draw(frame)

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
        Log.d(DepthAPIActivity.TAG, "Camera Tracking State: $trackingState")
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