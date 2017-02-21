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

        private static final int REGION_LEFT = 1;
        private static final int REGION_MENU = 2;
        private static final int REGION_RIGHT = 3;

        private int checkRegion(float x) {
           if (x < mView.getWidth() / 3) {
               return REGION_LEFT;
           } else if (x < mView.getWidth() * 2/3) {
               return REGION_MENU;
           } else
               return REGION_RIGHT;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Laz.yLog(TAG, "Double Tap");
            // Only accept 1/3 potion in the middle.
            if (checkRegion(e.getX()) != REGION_MENU) {
                // deal it as if double move page.
                onSingleTapConfirmed(e);
                onSingleTapConfirmed(e);
            }
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            switch (checkRegion(e.getX())) {
                case REGION_LEFT:
                    Laz.yLog(TAG, "Touch left");
                    mMotionListener.onMovePreviousPage();
                    break;
                case REGION_MENU:
                    Laz.yLog(TAG, "Touch middle");
                    mMotionListener.onOpenMenu();
                    break;
                case REGION_RIGHT:
                    Laz.yLog(TAG, "Touch right");
                    mMotionListener.onMoveNextPage();
                    break;
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            switch (checkRegion(e1.getX())) {
                case REGION_LEFT:
                    Laz.yLog(TAG, "Fling left");
                    mMotionListener.onMovePreviousPage();
                    break;
                case REGION_RIGHT:
                    Laz.yLog(TAG, "Fling right");
                    mMotionListener.onMoveNextPage();
                    break;
            }
            return true;
        }
    }
}
