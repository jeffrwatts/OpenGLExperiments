package com.jeffrwatts.openglexperiments

import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import com.google.ar.core.Session

class DisplayRotationHelper (context: Context) : DisplayManager.DisplayListener {

    private var viewportHeight = 0
    private var viewportWidth = 0
    private var viewPortChanged = false
    private val display = context.display
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    //private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun onResume() { displayManager.registerDisplayListener(this, null)}
    fun onPause() { displayManager.unregisterDisplayListener(this)}

    fun onSurfaceChanaged(width: Int, height: Int) {
        viewportWidth = width
        viewportHeight = height
        viewPortChanged = true
    }

    fun updateSessionIfNeeded(session: Session) {
        if (viewPortChanged) {
            display?.let {
                session.setDisplayGeometry(it.rotation, viewportWidth, viewportHeight)
            }
            viewPortChanged = false
        }
    }

    override fun onDisplayAdded(displayId: Int) {
    }

    override fun onDisplayRemoved(displayId: Int) {
    }

    override fun onDisplayChanged(displayId: Int) {
        viewPortChanged = true
    }
}