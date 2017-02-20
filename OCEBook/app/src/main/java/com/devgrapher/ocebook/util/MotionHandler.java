package com.devgrapher.ocebook.util;

import android.support.v4.util.Pair;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Hide implementations of motion handling.
 * This only provides motions that we desired through OnMotionListener.
 */

public class MotionHandler implements View.OnTouchListener {

    private static final String TAG = MotionHandler.class.toString();
    private final OnMotionListener mMotionListener;

    private Pair<Float, Float> mStartPos;

    public interface OnMotionListener {
        void onMoveNextPage();
        void onMovePreviousPage();
    }

    public MotionHandler(OnMotionListener listener) {
        mMotionListener = listener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (mStartPos != null) {
                    if (mStartPos.first < v.getWidth() / 2) {
                        Log.d(TAG, "Touch left");
                        mMotionListener.onMovePreviousPage();
                    } else {
                        Log.d(TAG, "Touch right");
                        mMotionListener.onMoveNextPage();
                    }
                    mStartPos = null;
                }
                break;
            case MotionEvent.ACTION_DOWN:
                mStartPos = Pair.create(event.getX(), event.getY());
                break;
            case MotionEvent.ACTION_CANCEL:
                mStartPos = null;
                break;
        }
        return false;
    }
}
