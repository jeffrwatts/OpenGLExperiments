package com.jeffrwatts.openglexperiments

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    private val buttonOpenGLFullScreen: Button by lazy { findViewById(R.id.buttonOpenGLFullScreen) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonOpenGLFullScreen.setOnClickListener { launchActivity(OpenGLES20FullScreenActivity::class.java)}
    }

    private fun launchActivity(klass: Class<*>) {
        val intent = Intent(this, klass)
        startActivity(intent)
    }
}