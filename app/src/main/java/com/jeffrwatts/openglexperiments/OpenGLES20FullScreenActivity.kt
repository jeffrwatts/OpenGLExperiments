package com.jeffrwatts.openglexperiments

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class OpenGLES20FullScreenActivity : AppCompatActivity() {
    private lateinit var gLView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use GLSurface View as the primary view for this activity.
        gLView = MyGLSurfaceView(this)
        setContentView(gLView)
    }
}