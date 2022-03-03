package com.jeffrwatts.openglexperiments.helpers

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import java.util.concurrent.ArrayBlockingQueue

class TapHelper (context: Context) : View.OnTouchListener {
    val queuedSingleTaps = ArrayBlockingQueue<MotionEvent>(16)
    val gestureDetector = GestureDetector(context, object: GestureDetector.OnGestureListener{

        override fun onSingleTapUp(e: MotionEvent?): Boolean {
            queuedSingleTaps.offer(e)
            return true
        }
        override fun onDown(e: MotionEvent?): Boolean {return true}
        override fun onShowPress(e: MotionEvent?) {}
        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float ): Boolean {return true}
        override fun onLongPress(e: MotionEvent?) {}
        override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {return true}
    })

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        v?.performClick()
        return gestureDetector.onTouchEvent(event)
    }

    fun poll(): MotionEvent? { return queuedSingleTaps.poll() }
}