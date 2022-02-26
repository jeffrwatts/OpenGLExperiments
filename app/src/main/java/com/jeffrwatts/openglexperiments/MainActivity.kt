package com.jeffrwatts.openglexperiments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.ar.core.ArCoreApk
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 1
    }

    private val buttonOpenGLFullScreen: Button by lazy { findViewById(R.id.buttonOpenGLFullScreen) }
    private val buttonCheckARCoreAvailable: Button by lazy { findViewById(R.id.buttonCheckARCoreAvailable) }
    private val buttonDepthAPI: Button by lazy { findViewById(R.id.buttonDepthAPI) }
    private val buttonRawDepthAPI: Button by lazy { findViewById(R.id.buttonRawDepthAPI) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonOpenGLFullScreen.setOnClickListener { launchActivity(OpenGLES20FullScreenActivity::class.java)}

        buttonCheckARCoreAvailable.setOnClickListener { checkARCoreAvailability() }

        buttonDepthAPI.isEnabled = false
        buttonDepthAPI.setOnClickListener { launchActivity(DepthAPIActivity::class.java)}

        buttonRawDepthAPI.isEnabled = false
        buttonRawDepthAPI.setOnClickListener { launchActivity(RawDepthAPIActivity::class.java)}

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            onCameraPermissionsGranted()
        }
    }

    private fun checkARCoreAvailability () {
        val arCoreInstance = ArCoreApk.getInstance()
        val availability = arCoreInstance.checkAvailability(this)

        if (availability.isTransient) {
            Toast.makeText(this, "ARCore checkAvailability isTransient is true", Toast.LENGTH_LONG).show()
        } else if (availability.isSupported) {
            buttonDepthAPI.isEnabled = true
            buttonRawDepthAPI.isEnabled = true
        } else {
            Toast.makeText(this, "ARCore is not available.", Toast.LENGTH_LONG).show()
        }

        if (availability.isSupported) {
            try {
                when (arCoreInstance.requestInstall(this, true)) {
                    ArCoreApk.InstallStatus.INSTALLED -> {
                        Toast.makeText(this, "GPS up to date for ARCore.", Toast.LENGTH_LONG).show()
                    }
                    ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                        Toast.makeText(this, "GPS out of date for ARCore.", Toast.LENGTH_LONG).show()
                    }
                    else -> {
                        Toast.makeText(this, "Unexpected result checking for GPS constraint.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception thrown while checking GPS constraint", e)
            }
        }
    }



    private fun onCameraPermissionsGranted() {
        // Check AR Core availability.
        checkARCoreAvailability()
    }

    private fun launchActivity(klass: Class<*>) {
        val intent = Intent(this, klass)
        startActivity(intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            onCameraPermissionsGranted()
        } else {
            Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_LONG).show()
        }
    }
}