package com.jeffrwatts.openglexperiments.renderers

import android.media.Image
import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Log
import com.google.ar.core.Frame
import com.google.ar.core.exceptions.NotYetAvailableException
import java.nio.ByteOrder

class DepthTextureHandler {
    companion object {
        const val TAG = "DepthTextureHandler"
    }

    var depthTexture = -1
    var depthWidth = -1
    var depthHeight = -1

    fun createOnGlThread() {
        val textureId = IntArray(1)
        GLES20.glGenTextures(1, textureId, 0)
        depthTexture = textureId[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTexture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    }

    fun update(frame: Frame) {
        try {
            val depthImage = frame.acquireDepthImage()

            val x = depthImage.width / 2
            val y = depthImage.height / 2
            val distance = getMillimetersDepth(depthImage, x, y)
            Log.d(TAG, "Distance at $x, $y, is $distance mm")

            depthWidth = depthImage.width
            depthHeight = depthImage.height
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, depthTexture)
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                GLES30.GL_RG8,
                depthWidth,
                depthHeight,
                0,
                GLES30.GL_RG,
                GLES20.GL_UNSIGNED_BYTE,
                depthImage.planes[0].buffer
            )
            depthImage.close()
        } catch (e: NotYetAvailableException) {
            // This normally means that depth data is not available yet.
            Log.e(TAG, "Exception trying to get depth image", e)
        }
    }

    private fun getMillimetersDepth(depthImage: Image, x: Int, y: Int): Int {
        // The depth image has a single plane, which stores depth for each
        // pixel as 16-bit unsigned integers.
        val plane: Image.Plane = depthImage.getPlanes().get(0)
        val byteIndex: Int = x * plane.pixelStride + y * plane.rowStride
        val buffer = plane.buffer.order(ByteOrder.nativeOrder())
        return buffer.getShort(byteIndex).toInt()
    }
}