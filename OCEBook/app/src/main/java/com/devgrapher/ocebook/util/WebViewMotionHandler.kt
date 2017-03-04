package com.devgrapher.ocebook.util

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

/**
 * Hide implementations of motion handling.
 * This only provides motions that we desired through OnMotionListener.
 */

class WebViewMotionHandler(context: Context, private val mMotionListener: WebViewMotionHandler.OnMotionListener) : View.OnTouchListener {
    private val mGestureDetector: GestureDetector

    private var mView: View? = null

    interface OnMotionListener {
        fun onMoveNextPage()
        fun onMovePreviousPage()
        fun onOpenMenu()
    }

    init {
        mGestureDetector = GestureDetector(context, GestureListener())
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        mView = v
        return mGestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        private fun checkRegion(x: Float): Int {
            if (x < mView!!.width / 3) {
                return REGION_LEFT
            } else if (x < mView!!.width * 2 / 3) {
                return REGION_MENU
            } else
                return REGION_RIGHT
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            Laz.yLog(TAG, "Double Tap")
            // Only accept 1/3 potion in the middle.
            if (checkRegion(e.x) != REGION_MENU) {
                // deal it as if double move page.
                onSingleTapConfirmed(e)
                onSingleTapConfirmed(e)
            }
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            when (checkRegion(e.x)) {
                REGION_LEFT -> {
                    Laz.yLog(TAG, "Touch left")
                    mMotionListener.onMovePreviousPage()
                }
                REGION_MENU -> {
                    Laz.yLog(TAG, "Touch middle")
                    mMotionListener.onOpenMenu()
                }
                REGION_RIGHT -> {
                    Laz.yLog(TAG, "Touch right")
                    mMotionListener.onMoveNextPage()
                }
            }
            return true
        }

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            when (checkRegion(e1.x)) {
                REGION_LEFT -> {
                    Laz.yLog(TAG, "Fling left")
                    mMotionListener.onMovePreviousPage()
                }
                REGION_RIGHT -> {
                    Laz.yLog(TAG, "Fling right")
                    mMotionListener.onMoveNextPage()
                }
            }
            return true
        }

        private val REGION_LEFT = 1
        private val REGION_MENU = 2
        private val REGION_RIGHT = 3
    }

    companion object {

        private val TAG = WebViewMotionHandler::class.java.toString()
    }
}
