package com.jeffrwatts.openglexperiments

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.MotionEvent

class MyGLSurfaceView (context: Context) :GLSurfaceView(context) {
    private val renderer: MyGLRenderer
    private var previousX: Float = 0f
    private var previousY: Float = 0f
    private val touchScaleFactor = 180.0f / 320f

    init {
        setEGLContextClientVersion(2)
        renderer = MyGLRenderer()
        setRenderer(renderer)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                var dx = event.x - previousX
                var dy = event.y - previousY

                if (y > height / 2) {
                    dx*=-1
                }

                if (x < width / 2) {
                    dy *=-1
                }

                renderer.angle += (dx + dy) * touchScaleFactor
                requestRender()
            }
        }

        previousX = x
        previousY = y
        return true
    }
}