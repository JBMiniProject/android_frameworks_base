/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2013 JB Mini Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.widget.jbminiproject;

import java.util.ArrayList;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.android.internal.R;
import com.android.internal.widget.DrawableHolder;

public class BB10View extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final String TAG = "BB10View";

    // Lock state machine states
    private static final int STATE_RESET_LOCK = 0;
    private static final int STATE_READY = 1;
    private static final int STATE_START_ATTEMPT = 2;
    private static final int STATE_ATTEMPTING = 3;
    private static final int STATE_UNLOCK_ATTEMPT = 4;
    private static final int STATE_UNLOCK_SUCCESS = 5;

    // Animation properties.
    private static final long VIBRATE_SHORT = 0;
    private static final long VIBRATE_LONG = 0;
    private static final long DURATION = 300;
    private static final long FINAL_DURATION = 350;
    private static final long RING_DELAY = 1300;
    private static final long FINAL_DELAY = 200;
    private static final long SHORT_DELAY = 100;
    private static final long RESET_TIMEOUT = 300;

    private static final float GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_DISABLED = 0.5f;
    private static final float GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_ENABLED = 1.0f;

    private Vibrator mVibrator;
    private OnTriggerListener mOnTriggerListener;
    private ArrayList<DrawableHolder> mDrawables = new ArrayList<DrawableHolder>(4);
    private boolean mFingerDown = false;
    private float mRingRadius = 1280.0f;
    private int mSnapRadius = 200;
    private int mPos1 = 960;
    private int mPos2 = 880;
    private int mPos3 = 640;
    private int mPos4 = 55;
    private float mLockCenterX;
    private float mLockCenterY;
    private float mMouseX;
    private float mMouseY;
    private DrawableHolder mUnlockRing;
    private DrawableHolder mUnlockWave;
    private DrawableHolder mUnlockDefault;
    private DrawableHolder mUnlockHalo;
    private int mLockState = STATE_RESET_LOCK;
    private int mGrabbedState = OnTriggerListener.NO_HANDLE;
    private int densityDpi = Integer.parseInt(android.os.SystemProperties.get("ro.sf.lcd_density")); 

    public BB10View(Context context) {
        this(context, null);
    }

    public BB10View(Context context, AttributeSet attrs) {
        super(context, attrs);

        initDrawables();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mLockCenterX = 0.5f * w;
        mLockCenterY = 0.5f * h;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected int getSuggestedMinimumWidth() {
        return mUnlockRing.getWidth() + mUnlockHalo.getWidth();
    }

    @Override
    protected int getSuggestedMinimumHeight() {
        return mUnlockRing.getHeight() + mUnlockHalo.getHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
        int width;
        int height;

        if (widthSpecMode == MeasureSpec.AT_MOST) {
            width = Math.min(widthSpecSize, getSuggestedMinimumWidth());
        } else if (widthSpecMode == MeasureSpec.EXACTLY) {
            width = widthSpecSize;
        } else {
            width = getSuggestedMinimumWidth();
        }

        if (heightSpecMode == MeasureSpec.AT_MOST) {
            height = Math.min(heightSpecSize, getSuggestedMinimumWidth());
        } else if (heightSpecMode == MeasureSpec.EXACTLY) {
            height = heightSpecSize;
        } else {
            height = getSuggestedMinimumHeight();
        }

        setMeasuredDimension(width, height);
    }

    private void initDrawables() {
        if (densityDpi <= 120) {
            mRingRadius = 320.0f;
            mSnapRadius = 50;
            mPos1 = 240;
            mPos2 = 220;
            mPos3 = 160;
            mPos4 = 14;
        } else if (densityDpi <= 180 && densityDpi > 120) {
            mRingRadius = 480.0f;
            mSnapRadius = 75;
            mPos1 = 360;
            mPos2 = 330;
            mPos3 = 240;
            mPos4 = 21;
        }

        mUnlockRing = new DrawableHolder(createDrawable(R.drawable.unlock_ring_bb10));
        mUnlockRing.setX(mLockCenterX);
        mUnlockRing.setY(mLockCenterY);
        mUnlockRing.setScaleX(1.0f);
        mUnlockRing.setScaleY(1.0f);
        mUnlockRing.setAlpha(0.0f);
        mDrawables.add(mUnlockRing);

        mUnlockWave = new DrawableHolder(createDrawable(R.drawable.unlock_wave_bb10));
        mUnlockWave.setX(mLockCenterX);
        mUnlockWave.setY(mLockCenterY);
        mUnlockWave.setScaleX(1.0f);
        mUnlockWave.setScaleY(1.0f);
        mUnlockWave.setAlpha(1.0f);
        mDrawables.add(mUnlockWave);

        mUnlockDefault = new DrawableHolder(createDrawable(R.drawable.unlock_default_bb10));
        mUnlockDefault.setX(mLockCenterX);
        mUnlockDefault.setY(mLockCenterY);
        mUnlockDefault.setScaleX(1.0f);
        mUnlockDefault.setScaleY(1.0f);
        mUnlockDefault.setAlpha(1.0f);
        mDrawables.add(mUnlockDefault);

        mUnlockHalo = new DrawableHolder(createDrawable(R.drawable.unlock_halo_bb10));
        mUnlockHalo.setX(mLockCenterX);
        mUnlockHalo.setY(mLockCenterY + mPos3);
        mUnlockHalo.setScaleX(2.0f);
        mUnlockHalo.setScaleY(1.0f);
        mUnlockHalo.setAlpha(0.0f);
        mDrawables.add(mUnlockHalo);
    }

    private void waveUpdateFrame(float mouseX, float mouseY, boolean fingerDown) {
        double distX = mouseX - mLockCenterX;
        double distY = mouseY - mLockCenterY;
        int dragDistance = (int) Math.ceil(Math.hypot(distX, distY));
        double touchA = Math.atan2(distX, distY);
        float ringX = (float) (mLockCenterX + mRingRadius * Math.sin(touchA));
        float ringY = (float) (mLockCenterY + mRingRadius * Math.cos(touchA));

        switch (mLockState) {
            case STATE_RESET_LOCK:
                mUnlockRing.removeAnimationFor("x");
                mUnlockRing.removeAnimationFor("y");
                mUnlockRing.removeAnimationFor("scaleX");
                mUnlockRing.removeAnimationFor("scaleY");
                mUnlockRing.removeAnimationFor("alpha");
                mUnlockRing.setX(mLockCenterX);
                mUnlockRing.setY(mLockCenterY);
                mUnlockRing.setScaleY(1.0f);
                mUnlockRing.setScaleY(1.0f);
                mUnlockRing.setAlpha(0.0f);

                mUnlockWave.removeAnimationFor("x");
                mUnlockWave.removeAnimationFor("y");
                mUnlockWave.removeAnimationFor("scaleX");
                mUnlockWave.removeAnimationFor("scaleY");
                mUnlockWave.removeAnimationFor("alpha");
                mUnlockWave.setScaleX(1.0f);
                mUnlockWave.setScaleY(1.0f);
                mUnlockWave.setAlpha(1.0f);
                mUnlockWave.addAnimTo(DURATION, 0, "x", mLockCenterX, true);
                mUnlockWave.addAnimTo(DURATION, 0, "y", mLockCenterY, true);

                mUnlockDefault.removeAnimationFor("x");
                mUnlockDefault.removeAnimationFor("y");
                mUnlockDefault.removeAnimationFor("scaleX");
                mUnlockDefault.removeAnimationFor("scaleY");
                mUnlockDefault.removeAnimationFor("alpha");
                mUnlockDefault.setX(mLockCenterX);
                mUnlockDefault.setY(mLockCenterY);
                mUnlockDefault.setScaleX(1.0f);
                mUnlockDefault.setScaleY(1.0f);
                mUnlockDefault.setAlpha(1.0f);

                mUnlockHalo.removeAnimationFor("x");
                mUnlockHalo.removeAnimationFor("y");
                mUnlockHalo.removeAnimationFor("scaleX");
                mUnlockHalo.removeAnimationFor("scaleY");
                mUnlockHalo.removeAnimationFor("alpha");
                mUnlockHalo.setScaleX(2.0f);
                mUnlockHalo.setScaleY(1.0f);
                mUnlockHalo.addAnimTo(DURATION, 0, "x", mLockCenterX, true);
                mUnlockHalo.addAnimTo(DURATION, 0, "y", mLockCenterY + mPos2, true);
                mUnlockHalo.addAnimTo(0, DURATION, "y", mLockCenterY + mPos3, true);
                mUnlockHalo.addAnimTo(0, DURATION, "alpha", 0.0f, true);

                removeCallbacks(mLockTimerActions);

                mLockState = STATE_READY;
                break;

            case STATE_READY:
                break;

            case STATE_START_ATTEMPT:
                mLockState = STATE_ATTEMPTING;
                break;

            case STATE_ATTEMPTING:
                if (mouseY < mLockCenterY - mPos4) {
                    if (fingerDown) {
                        mUnlockWave.addAnimTo(0, 0, "x", mLockCenterX, true);
                        mUnlockWave.addAnimTo(0, 0, "y", mouseY - mPos2, true);
                        mUnlockWave.addAnimTo(0, 0, "scaleX", 1.0f, true);
                        mUnlockWave.addAnimTo(0, 0, "scaleY", 1.0f, true);
                        mUnlockWave.addAnimTo(0, 0, "alpha", 1.0f, true);

                        mUnlockDefault.addAnimTo(0, 0, "x", mLockCenterX, true);
                        mUnlockDefault.addAnimTo(0, 0, "y", mLockCenterY, true);
                        mUnlockDefault.addAnimTo(0, 0, "scaleX", 1.0f, true);
                        mUnlockDefault.addAnimTo(0, 0, "scaleY", 1.0f, true);
                        mUnlockDefault.addAnimTo(0, 0, "alpha", 0.0f, true);

                        mUnlockHalo.addAnimTo(0, 0, "x", mouseX, true);
                        mUnlockHalo.addAnimTo(0, 0, "y", mouseY, true);
                        mUnlockHalo.addAnimTo(0, 0, "scaleX", 2.0f, true);
                        mUnlockHalo.addAnimTo(0, 0, "scaleY", 1.0f, true);
                        mUnlockHalo.addAnimTo(0, 0, "alpha", 1.0f, true);
                    }  else {
                        mLockState = STATE_UNLOCK_ATTEMPT;
                    }
                } else {
                    mUnlockWave.addAnimTo(0, 0, "x", mLockCenterX, true);
                    mUnlockWave.addAnimTo(0, 0, "y", mouseY - mPos2, true);
                    mUnlockWave.addAnimTo(0, 0, "scaleX", 1.0f, true);
                    mUnlockWave.addAnimTo(0, 0, "scaleY", 1.0f, true);
                    mUnlockWave.addAnimTo(0, 0, "alpha", 1.0f, true);

                    mUnlockDefault.addAnimTo(0, 0, "x", mLockCenterX, true);
                    mUnlockDefault.addAnimTo(0, 0, "y", mLockCenterY, true);
                    mUnlockDefault.addAnimTo(0, 0, "scaleX", 1.0f, true);
                    mUnlockDefault.addAnimTo(0, 0, "scaleY", 1.0f, true);
                    mUnlockDefault.addAnimTo(0, 0, "alpha", 1.0f, true);

                    mUnlockHalo.addAnimTo(0, 0, "x", mouseX, true);
                    mUnlockHalo.addAnimTo(0, 0, "y", mouseY, true);
                    mUnlockHalo.addAnimTo(0, 0, "scaleX", 2.0f, true);
                    mUnlockHalo.addAnimTo(0, 0, "scaleY", 1.0f, true);
                    mUnlockHalo.addAnimTo(0, 0, "alpha", 1.0f, true);
                }
                break;

            case STATE_UNLOCK_ATTEMPT:
                if (mouseY < mLockCenterY - mPos4) {
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "x", mLockCenterX, true);
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "y", mLockCenterY - 1280, true);
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "scaleX", 2.0f, false);
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "scaleY", 1.0f, false);
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "alpha", 1.0f, false);

                    mUnlockHalo.addAnimTo(FINAL_DURATION, 0, "x", mLockCenterX, true);
                    mUnlockHalo.addAnimTo(FINAL_DURATION, 0, "y", mLockCenterY - mPos1, true);
                    mUnlockHalo.addAnimTo(FINAL_DURATION, 0, "scaleX", 1.0f, false);
                    mUnlockHalo.addAnimTo(FINAL_DURATION, 0, "scaleY", 1.0f, false);
                    mUnlockHalo.addAnimTo(FINAL_DURATION, 0, "alpha", 1.0f, false);

                    removeCallbacks(mLockTimerActions);

                    postDelayed(mLockTimerActions, RESET_TIMEOUT);

                    dispatchTriggerEvent(OnTriggerListener.CENTER_HANDLE);
                    mLockState = STATE_UNLOCK_SUCCESS;
                } else {
                    mLockState = STATE_RESET_LOCK;
                }
                break;

            case STATE_UNLOCK_SUCCESS:
                break;

            default:
                break;
        }
        mUnlockDefault.startAnimations(this);
        mUnlockHalo.startAnimations(this);
        mUnlockRing.startAnimations(this);
        mUnlockWave.startAnimations(this);
    }

    BitmapDrawable createDrawable(int resId) {
        Resources res = getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(res, resId);
        return new BitmapDrawable(res, bitmap);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        waveUpdateFrame(mMouseX, mMouseY, mFingerDown);
        for (int i = 0; i < mDrawables.size(); ++i) {
            mDrawables.get(i).draw(canvas);
        }
    }

    private final Runnable mLockTimerActions = new Runnable() {
        public void run() {
            if (mLockState == STATE_ATTEMPTING) {
                mLockState = STATE_RESET_LOCK;
            }

            invalidate();
        }
    };

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if (AccessibilityManager.getInstance(mContext).isTouchExplorationEnabled()) {
            final int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    event.setAction(MotionEvent.ACTION_DOWN);
                    break;
                case MotionEvent.ACTION_HOVER_MOVE:
                    event.setAction(MotionEvent.ACTION_MOVE);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    event.setAction(MotionEvent.ACTION_UP);
                    break;
            }
            onTouchEvent(event);
            event.setAction(action);
        }
        return super.onHoverEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        mMouseX = event.getX();
        mMouseY = event.getY();
        boolean handled = false;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                removeCallbacks(mLockTimerActions);
                mFingerDown = true;
                tryTransitionToStartAttemptState(event);
                handled = true;
                break;

            case MotionEvent.ACTION_MOVE:
                tryTransitionToStartAttemptState(event);
                handled = true;
                break;

            case MotionEvent.ACTION_UP:
                mFingerDown = false;
                postDelayed(mLockTimerActions, RESET_TIMEOUT);
                setGrabbedState(OnTriggerListener.NO_HANDLE);

                waveUpdateFrame(mMouseX, mMouseY, mFingerDown);
                handled = true;
                break;

            case MotionEvent.ACTION_CANCEL:
                mFingerDown = false;
                handled = true;
                break;
        }
        invalidate();
        return handled ? true : super.onTouchEvent(event);
    }

    private void tryTransitionToStartAttemptState(MotionEvent event) {
        final float dx = event.getX() - mUnlockHalo.getX();
        final float dy = event.getY() - mUnlockHalo.getY();
        float dist = (float) Math.hypot(dx, dy);
        if (dist <= getScaledGrabHandleRadius()) {
            setGrabbedState(OnTriggerListener.CENTER_HANDLE);
            if (mLockState == STATE_READY) {
                mLockState = STATE_START_ATTEMPT;
                if (AccessibilityManager.getInstance(mContext).isEnabled()) {
                    announceUnlockHandle();
                }
            }
        }
    }

    private float getScaledGrabHandleRadius() {
        if (AccessibilityManager.getInstance(mContext).isEnabled()) {
            return GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_ENABLED * mUnlockHalo.getWidth();
        } else {
            return GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_DISABLED * mUnlockHalo.getWidth();
        }
    }

    private void announceUnlockHandle() {
        setContentDescription(mContext.getString(R.string.description_target_unlock_tablet));
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED);
        setContentDescription(null);
    }

    private synchronized void vibrate(long duration) {
        if (mVibrator == null) {
            mVibrator = (android.os.Vibrator)
                    getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }
        mVibrator.vibrate(duration);
    }

    public void setOnTriggerListener(OnTriggerListener listener) {
        mOnTriggerListener = listener;
    }

    private void dispatchTriggerEvent(int whichHandle) {
        vibrate(VIBRATE_LONG);
        if (mOnTriggerListener != null) {
            mOnTriggerListener.onTrigger(this, whichHandle);
        }
    }

    private void setGrabbedState(int newState) {
        if (newState != mGrabbedState) {
            mGrabbedState = newState;
            if (mOnTriggerListener != null) {
                mOnTriggerListener.onGrabbedStateChange(this, mGrabbedState);
            }
        }
    }

    public interface OnTriggerListener {
        public static final int NO_HANDLE = 0;
        public static final int CENTER_HANDLE = 10;

        void onTrigger(View v, int whichHandle);

        void onGrabbedStateChange(View v, int grabbedState);
    }

    public void onAnimationUpdate(ValueAnimator animation) {
        invalidate();
    }

    public void reset() {
        mLockState = STATE_RESET_LOCK;
        invalidate();
    }
}
