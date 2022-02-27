package com.jeffrwatts.openglexperiments

import android.media.Image
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.Anchor
import com.google.ar.core.CameraIntrinsics
import com.google.ar.core.Frame
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.experimental.and
import kotlin.math.ceil
import kotlin.math.sqrt

object DepthData {
    const val TAG = "DepthData"
    const val FLOATS_PER_POINT = 4 // X,Y,Z,confidence.


    fun create(frame: Frame, cameraPoseAnchor: Anchor): FloatBuffer? {
        try {
            val depthImage = frame.acquireRawDepthImage()
            val confidenceImage = frame.acquireRawDepthConfidenceImage()

            val intrinsics = frame.camera.textureIntrinsics
            val modelMatrix = FloatArray(16)
            cameraPoseAnchor.pose.toMatrix(modelMatrix, 0)
            val points = convertRawDepthImagesTo3dPointBuffer(depthImage, confidenceImage, intrinsics, modelMatrix)
            depthImage.close()
            confidenceImage.close()
            return points
        } catch (e: Exception) {
            Log.e(TAG, "Exception thrown getting depth data", e)
        }
        return null
    }

    private fun convertRawDepthImagesTo3dPointBuffer(depth: Image, confidence: Image, cameraTextureIntrinsics: CameraIntrinsics, modelMatrix: FloatArray): FloatBuffer {
        val depthImagePlane = depth.planes[0]
        val depthByteBufferOriginal = depthImagePlane.buffer
        val depthByteBuffer = ByteBuffer.allocate(depthByteBufferOriginal.capacity())
        depthByteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        while (depthByteBufferOriginal.hasRemaining()) {
            depthByteBuffer.put(depthByteBufferOriginal.get())
        }
        depthByteBuffer.rewind()

        val depthBuffer = depthByteBuffer.asShortBuffer()

        val confidenceImagePlane = confidence.planes[0]
        val confidenceBufferOriginal = confidenceImagePlane.buffer
        val confidenceBuffer = ByteBuffer.allocate(confidenceBufferOriginal.capacity())
        confidenceBuffer.order(ByteOrder.LITTLE_ENDIAN)
        while(confidenceBufferOriginal.hasRemaining()) {
            confidenceBuffer.put(confidenceBufferOriginal.get())
        }
        confidenceBuffer.rewind()

        // To transform 2D depth pixels into 3D points, retrieve the intrinsic camera parameters
        // corresponding to the depth image. See more information about the depth values at
        // https://developers.google.com/ar/develop/java/depth/overview#understand-depth-values.
        val intrinsicsDimensions = cameraTextureIntrinsics.imageDimensions
        val depthWidth = depth.width
        val depthHeight = depth.height
        val fx = cameraTextureIntrinsics.focalLength[0] * depthWidth / intrinsicsDimensions[0]
        val fy = cameraTextureIntrinsics.focalLength[1] * depthHeight / intrinsicsDimensions[1]
        val cx = cameraTextureIntrinsics.principalPoint[0] * depthWidth / intrinsicsDimensions[0]
        val cy = cameraTextureIntrinsics.principalPoint[1] * depthHeight / intrinsicsDimensions[1]

        // Allocate the destination point buffer. If the number of depth pixels is larger than
        // `maxNumberOfPointsToRender` we uniformly subsample. The raw depth image may have
        // different resolutions on different devices.
        val maxNumberOfPointsToRender = 20000
        val step = ceil(sqrt((depthWidth.toDouble() * depthHeight.toDouble() / maxNumberOfPointsToRender.toDouble()))).toInt()
        val points = FloatBuffer.allocate((depthWidth / step * depthHeight / step * FLOATS_PER_POINT))

        val pointCamera = FloatArray(4)
        val pointWorld = FloatArray(4)

        for (y in 0 until depthHeight step step) {
           for (x in 0 until depthWidth step step) {
               val depthMillimeters = depthBuffer.get(y*depthWidth + x) // Depth image pixels are in mm.
               if (depthMillimeters.toInt() == 0) {
                   // Pixels with value zero are invalid, meaning depth estimates are missing from
                   // this location.
                   continue
               }

               val depthMeters = depthMillimeters / 1000.0f // Depth image pixels are in mm.

               // Retrieve the confidence value for this pixel.
               val confidencePixelValue = confidenceBuffer.get(y*confidenceImagePlane.rowStride + x * confidenceImagePlane.pixelStride)
               val confidenceNormalized = (confidencePixelValue.and(0xff.toByte())).toFloat() / 255.0f

               // Unproject the depth into a 3D point in camera coordinates.
               pointCamera[0] = depthMeters * (x - cx) / fx
               pointCamera[1] = depthMeters * (cy - y) / fy
               pointCamera[2] = -depthMeters
               pointCamera[3] = 1F

               // Apply model matrix to transform point into world coordinates.
               Matrix.multiplyMV(pointWorld, 0, modelMatrix, 0, pointCamera, 0)
               points.put(pointWorld[0]) // X
               points.put(pointWorld[1]) // Y
               points.put(pointWorld[2]) // Z
               points.put(confidenceNormalized)
            }
        }

        points.rewind()
        return points
    }
}