package com.jeffrwatts.openglexperiments

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Triangle {
    private val triangleCoords = floatArrayOf(     // in counterclockwise order:
        0.0f, 0.622008459f, 0.0f,      // top
        -0.5f, -0.311004243f, 0.0f,    // bottom left
        0.5f, -0.311004243f, 0.0f      // bottom right
    )

    val coordsPerVertex = 3
    val vertexStride = coordsPerVertex * 4
    val vertexCount = triangleCoords.size / coordsPerVertex
    val color = floatArrayOf(0.63671875f, 0.76953125f, 0.22265625f, 1.0f)

    private var vertexBuffer: FloatBuffer =
        // (number of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(triangleCoords.size * 4).run {
            // use the device hardware's native byte order
            order(ByteOrder.nativeOrder())

            // create a floating point buffer from the ByteBuffer
            asFloatBuffer().apply {
                // add the coordinates to the FloatBuffer
                put(triangleCoords)
                // set the buffer to read the first coordinate
                position(0)
            }
        }

    private var program: Int

    init {
        program = loadProgram()
    }

    fun draw(vPMatrix: FloatArray) {
        // Add the program to the OpenGL ES environment.
        GLES20.glUseProgram(program)

        // Get a handle to the vertex shader's vPosition member.
        var positionHandle = GLES20.glGetAttribLocation(program, "vPosition").also { positionHandle->
            // Enable a handle to the triangle vertices.
            GLES20.glEnableVertexAttribArray(positionHandle)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(positionHandle, coordsPerVertex, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer)

            // Get handle to fragment's vColor member
            GLES20.glGetUniformLocation(program, "vColor").also { colorHandle->
                // Set color for drawing the triangle.
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            GLES20.glGetUniformLocation(program, "uMVPMatrix").also { matrixHandle->
                GLES20.glUniformMatrix4fv(matrixHandle, 1, false, vPMatrix, 0)
            }

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(positionHandle)
        }
    }

    private fun loadProgram (): Int {
        val vertexShaderCode =
        // This matrix member variable provides a hook to manipulate
            // the coordinates of the objects that use this vertex shader
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 vPosition;" +
                    "void main() {" +
                    // the matrix must be included as a modifier of gl_Position
                    // Note that the uMVPMatrix factor *must be first* in order
                    // for the matrix multiplication product to be correct.
                    "  gl_Position = uMVPMatrix * vPosition;" +
                    "}"

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)

        val fragmentShaderCode =
            "precision mediump float;" +
                    "uniform vec4 vColor;" +
                    "void main() {" +
                    "  gl_FragColor = vColor;" +
                    "}"
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        return GLES20.glCreateShader(type).also { shader ->

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
