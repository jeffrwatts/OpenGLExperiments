package com.jeffrwatts.openglexperiments

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Camera
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class BoxRenderer {
    companion object {
        private const val TAG = "BoxRenderer"
        private const val VERTEX_SHADER_NAME = "shaders/box.vert"
        private const val FRAGMENT_SHADER_NAME = "shaders/box.frag"
    }

    private lateinit var vertexBuffer: FloatBuffer
    private lateinit var indexBuffer: ShortBuffer

    private var program = 0
    private var vPosition = 0
    private var uViewProjection = 0

    public fun createOnGlThread(context: Context) {
        ShaderUtil.checkGLError(TAG, "Create")
        program = ShaderUtil.loadProgram(TAG, context, VERTEX_SHADER_NAME, FRAGMENT_SHADER_NAME)
        ShaderUtil.checkGLError(TAG, "Program")

        vPosition = GLES20.glGetAttribLocation(program, "vPosition")
        uViewProjection = GLES20.glGetUniformLocation(program, "uViewProjection")

        // Sets the index buffer, which is constant regardless of the size/position of the cube.
        val indices = shortArrayOf(
            0, 1, 2, 2, 1, 3,  // Front.
            4, 5, 6, 6, 5, 7,  // Back.
            8, 9, 10, 10, 9, 11,  // Left.
            12, 13, 14, 14, 13, 15,  // Right.
            16, 17, 18, 18, 17, 19,  // Top.
            20, 21, 22, 22, 21, 23 // Bottom.
        )
        val indexByteBuffer = ByteBuffer.allocateDirect(2 * indices.size) // 2 bytes per short.

        indexByteBuffer.order(ByteOrder.nativeOrder())
        indexBuffer = indexByteBuffer.asShortBuffer()
        indexBuffer.rewind()
        indexBuffer.put(indices)

        // Allocates vertexBuffer.
        val vertexByteBuffer = ByteBuffer.allocateDirect(288) // 6 faces * 4 corners * 3 dimensions * 4 bytes-per-float.

        vertexByteBuffer.order(ByteOrder.nativeOrder())
        vertexBuffer = vertexByteBuffer.asFloatBuffer()
        ShaderUtil.checkGLError(BoxRenderer.TAG, "Init complete")
    }

    fun draw(aabb: AABB, camera: Camera) {
        val projectionMatrix = FloatArray(16)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)
        val viewMatrix = FloatArray(16)
        camera.getViewMatrix(viewMatrix, 0)
        val viewProjection = FloatArray(16)
        Matrix.multiplyMM(viewProjection, 0, projectionMatrix, 0, viewMatrix, 0)

        // Updates the positions of the cube.

        // Updates the positions of the cube.
        setCubeDimensions(aabb)
        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(uViewProjection, 1, false, viewProjection, 0)
        GLES20.glEnableVertexAttribArray(vPosition)
        vertexBuffer.rewind()
        GLES20.glVertexAttribPointer(
            vPosition, 3,
            GLES20.GL_FLOAT, false,
            12, vertexBuffer
        )
        ShaderUtil.checkGLError(TAG, "Draw")

        // Draws a cube.

        // Draws a cube.
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        indexBuffer.rewind()
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indexBuffer.remaining(),
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
        GLES20.glDisableVertexAttribArray(vPosition)
        ShaderUtil.checkGLError(TAG, "Draw complete")
    }

    private fun setCubeDimensions(aabb: AABB) {
        val vertices = floatArrayOf( // Front.
            aabb.minX, aabb.minY, aabb.maxZ,
            aabb.maxX, aabb.minY, aabb.maxZ,
            aabb.minX, aabb.maxY, aabb.maxZ,
            aabb.maxX, aabb.maxY, aabb.maxZ,  // Back.
            aabb.maxX, aabb.minY, aabb.minZ,
            aabb.minX, aabb.minY, aabb.minZ,
            aabb.maxX, aabb.maxY, aabb.minZ,
            aabb.minX, aabb.maxY, aabb.minZ,  // Left.
            aabb.minX, aabb.minY, aabb.minZ,
            aabb.minX, aabb.minY, aabb.maxZ,
            aabb.minX, aabb.maxY, aabb.minZ,
            aabb.minX, aabb.maxY, aabb.maxZ,  // Right.
            aabb.maxX, aabb.minY, aabb.maxZ,
            aabb.maxX, aabb.minY, aabb.minZ,
            aabb.maxX, aabb.maxY, aabb.maxZ,
            aabb.maxX, aabb.maxY, aabb.minZ,  // Top.
            aabb.minX, aabb.maxY, aabb.maxZ,
            aabb.maxX, aabb.maxY, aabb.maxZ,
            aabb.minX, aabb.maxY, aabb.minZ,
            aabb.maxX, aabb.maxY, aabb.minZ,  // Bottom.
            aabb.minX, aabb.minY, aabb.minZ,
            aabb.maxX, aabb.minY, aabb.minZ,
            aabb.minX, aabb.minY, aabb.maxZ,
            aabb.maxX, aabb.minY, aabb.maxZ
        )
        vertexBuffer.rewind()
        vertexBuffer.put(vertices)
    }
}