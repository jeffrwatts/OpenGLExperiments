package com.jeffrwatts.openglexperiments

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer : GLSurfaceView.Renderer {
    companion object {
        const val TAG = "MyGLRenderer"
    }

    @Volatile
    var angle: Float = 0f

    private val vPMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val cameraViewMatrix = FloatArray(16)
    private val rotationMatrix = FloatArray(16)
    private lateinit var triangle: Triangle

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d(TAG, "onSurfaceCreated")
        // Set background frame color.
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        triangle = Triangle()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged")
        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        // Using the width and height of the GLSurfaceView, create the projection transformation matrix that will be applied in onDrawFrame()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 7f)
    }

    override fun onDrawFrame(gl: GL10?) {
        Log.d(TAG, "onDrawFrame")
        // Redraw background color.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Set the camera position.
        Matrix.setLookAtM(cameraViewMatrix, 0, 0f, 0f, 3f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Perform Matrix multiplication of the projection and view transformation to get the final transformation matrix.
        Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, cameraViewMatrix, 0)

        // Create a rotation transformation for the triangle
        //val time = SystemClock.uptimeMillis() % 4000L
        //val angle = 0.090f * time.toInt()
        Matrix.setRotateM(rotationMatrix, 0, angle, 0f, 0f, -1.0f)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        val scratch = FloatArray(16)
        Matrix.multiplyMM(scratch, 0, vPMatrix, 0, rotationMatrix, 0)

        // Draw the triangle using this matrix transformation.
        triangle.draw(scratch)

    }
}