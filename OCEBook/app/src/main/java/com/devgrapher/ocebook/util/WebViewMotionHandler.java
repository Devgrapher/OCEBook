package com.devgrapher.ocebook.util;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Hide implementations of motion handling.
 * This only provides motions that we desired through OnMotionListener.
 */

public class WebViewMotionHandler implements View.OnTouchListener {

    private static final String TAG = WebViewMotionHandler.class.toString();

    private final OnMotionListener mMotionListener;
    private final GestureDetector mGestureDetector;

    private View mView;

    public interface OnMotionListener {
        void onMoveNextPage();
        void onMovePreviousPage();
        void onOpenMenu();
    }

    public WebViewMotionHandler(Context context, OnMotionListener listener) {
        mMotionListener = listener;
        mGestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mView = v;
        return mGestureDetector.onTouchEvent(event);
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Laz.yLog(TAG, "Double Tap");
            // Only accept 1/3 potion in the middle.
            if (e.getX() > mView.getWidth() / 3 &&
                    e.getX() < mView.getWidth() * 2/3) {
                mMotionListener.onOpenMenu();
            } else {
                // deal it as if double move page.
                onSingleTapConfirmed(e);
                onSingleTapConfirmed(e);
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (e.getX() < mView.getWidth() / 2) {
                Laz.yLog(TAG, "Touch left");
                mMotionListener.onMovePreviousPage();
            } else {
                Laz.yLog(TAG, "Touch right");
                mMotionListener.onMoveNextPage();
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (e1.getX() < mView.getWidth() / 2) {
                Laz.yLog(TAG, "Fling left");
                mMotionListener.onMovePreviousPage();
            } else {
                Laz.yLog(TAG, "Fling right");
                mMotionListener.onMoveNextPage();
            }
            return true;
        }
    }
}
