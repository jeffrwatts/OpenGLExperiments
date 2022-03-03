package com.jeffrwatts.openglexperiments.helpers

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object ShaderUtils {
    private const val TAG = "ShaderUtils"

    fun loadProgram (context: Context, vertexShaderFilename: String, fragmentShaderFilename: String): Int {
        val vertexShader = loadShaderFromAssets(context, GLES20.GL_VERTEX_SHADER, vertexShaderFilename)
        val fragmentShader = loadShaderFromAssets(context, GLES20.GL_FRAGMENT_SHADER, fragmentShaderFilename)

        return GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
            GLES20.glUseProgram(it)
            checkGLError()
        }
    }

    fun loadShader(type: Int, shaderCode: String): Int {
        var shader = GLES20.glCreateShader(type).also { shader ->
            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }

        // Get the compilation status.
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader))
            GLES20.glDeleteShader(shader)
            shader = 0
        }

        return shader
    }

    fun loadShaderFromAssets(context: Context, type: Int, filename: String): Int {
        val shaderCode = StringBuffer()
        try {
            val inputStream = context.assets.open(filename)
            val reader = BufferedReader(InputStreamReader(inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                shaderCode.append(line).append("\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception reading shader from Assets", e)
        }
        return loadShader(type, shaderCode.toString())
    }

    fun checkGLError() {
        var lastError = GLES20.GL_NO_ERROR
        // Drain the queue of all errors.
        var error: Int
        while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "glError $error")
            lastError = error
        }
    }
}