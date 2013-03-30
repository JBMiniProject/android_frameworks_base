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

public class AcerView extends View implements ValueAnimator.AnimatorUpdateListener {
    private static final String TAG = "AcerView";

    // Lock state machine states
    private static final int STATE_RESET_LOCK = 0;
    private static final int STATE_READY = 1;
    private static final int STATE_START_ATTEMPT = 2;
    private static final int STATE_ATTEMPTING = 3;
    private static final int STATE_UNLOCK_ATTEMPT = 4;
    private static final int STATE_UNLOCK_SUCCESS = 5;

    // Animation properties.
    private static final long VIBRATE_SHORT = 30;
    private static final long VIBRATE_LONG = 40;
    private static final long DURATION = 300;
    private static final long FINAL_DURATION = 500;
    private static final long RING_DELAY = 1300;
    private static final long FINAL_DELAY = 200;
    private static final long SHORT_DELAY = 100;
    private static final long RESET_TIMEOUT = 500;

    private static final float GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_DISABLED = 1.0f;
    private static final float GRAB_HANDLE_RADIUS_SCALE_ACCESSIBILITY_ENABLED = 1.0f;

    private Vibrator mVibrator;
    private OnTriggerListener mOnTriggerListener;
    private ArrayList<DrawableHolder> mDrawables = new ArrayList<DrawableHolder>(4);
    private boolean mFingerDown = false;
    private float mRingRadius = 520.0f;
    private int mSnapRadius = 255;
    private int mUnlockpos = 240;
    private float mLockCenterX;
    private float mLockCenterY;
    private float mMouseX;
    private float mMouseY;
    private DrawableHolder mUnlockRing;
    private DrawableHolder mUnlockDefault;
    private DrawableHolder mUnlockHalo;
    private DrawableHolder mUnlockWave;
    private int mLockState = STATE_RESET_LOCK;
    private int mGrabbedState = OnTriggerListener.NO_HANDLE;
    private int densityDpi = Integer.parseInt(android.os.SystemProperties.get("ro.sf.lcd_density"));

    public AcerView(Context context) {
        this(context, null);
    }

    public AcerView(Context context, AttributeSet attrs) {
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
            mRingRadius = 130.0f;
            mSnapRadius = 85;
            mUnlockpos = 80;
        } else if (densityDpi <= 180 && densityDpi > 120) {
            mRingRadius = 195.0f;
            mSnapRadius = 113;
            mUnlockpos = 107;
        }

        mUnlockRing = new DrawableHolder(createDrawable(R.drawable.unlock_ring_acer));
        mUnlockRing.setX(mLockCenterX - mUnlockpos);
        mUnlockRing.setY(mLockCenterY);
        mUnlockRing.setScaleX(0.0f);
        mUnlockRing.setScaleY(1.0f);
        mUnlockRing.setAlpha(1.0f);
        mDrawables.add(mUnlockRing);

        mUnlockDefault = new DrawableHolder(createDrawable(R.drawable.unlock_default_acer));
        mUnlockDefault.setX(mLockCenterX);
        mUnlockDefault.setY(mLockCenterY);
        mUnlockDefault.setScaleX(0.0f);
        mUnlockDefault.setScaleY(1.0f);
        mUnlockDefault.setAlpha(1.0f);
        mDrawables.add(mUnlockDefault);

        mUnlockWave = new DrawableHolder(createDrawable(R.drawable.unlock_wave_acer));
        mUnlockWave.setX(mLockCenterX + mUnlockpos);
        mUnlockWave.setY(mLockCenterY);
        mUnlockWave.setScaleX(0.0f);
        mUnlockWave.setScaleY(1.0f);
        mUnlockWave.setAlpha(1.0f);
        mDrawables.add(mUnlockWave);

        mUnlockHalo = new DrawableHolder(createDrawable(R.drawable.unlock_halo_acer));
        mUnlockHalo.setX(mLockCenterX);
        mUnlockHalo.setY(mLockCenterY);
        mUnlockHalo.setScaleX(1.0f);
        mUnlockHalo.setScaleY(1.0f);
        mUnlockHalo.setAlpha(1.0f);
        mDrawables.add(mUnlockHalo);
    }

    private void waveUpdateFrame(float mouseX, float mouseY, boolean fingerDown) {
        double distX = mouseX - mLockCenterX;
        double distY = mouseY - mLockCenterY;
        int dragDistance = (int) Math.ceil(Math.hypot(distX, distY));
        double touchA = Math.atan2(distX, distY);
        float ringX = (float) (mLockCenterX + mRingRadius * Math.sin(touchA));
        float ringY = (float) (mLockCenterY + mRingRadius * Math.cos(touchA));
        float shadescale = dragDistance * 2.0f / mRingRadius;
        if (shadescale >= 1.0f) {
            shadescale = 1.0f;
        }

        switch (mLockState) {
            case STATE_RESET_LOCK:
                mUnlockRing.removeAnimationFor("x");
                mUnlockRing.removeAnimationFor("y");
                mUnlockRing.removeAnimationFor("scaleX");
                mUnlockRing.removeAnimationFor("scaleY");
                mUnlockRing.removeAnimationFor("alpha");
                mUnlockRing.setX(mLockCenterX - mUnlockpos);
                mUnlockRing.setY(mLockCenterY);
                mUnlockRing.setScaleY(1.0f);
                mUnlockRing.setAlpha(1.0f);
                mUnlockRing.addAnimTo(DURATION, 0, "scaleX", 0.0f, true);

                mUnlockDefault.removeAnimationFor("x");
                mUnlockDefault.removeAnimationFor("y");
                mUnlockDefault.removeAnimationFor("scaleX");
                mUnlockDefault.removeAnimationFor("scaleY");
                mUnlockDefault.removeAnimationFor("alpha");
                mUnlockDefault.setX(mLockCenterX);
                mUnlockDefault.setY(mLockCenterY);
                mUnlockDefault.setScaleY(1.0f);
                mUnlockDefault.setAlpha(1.0f);
                mUnlockDefault.addAnimTo(DURATION, 0, "scaleX", 0.0f, true);

                mUnlockWave.removeAnimationFor("x");
                mUnlockWave.removeAnimationFor("y");
                mUnlockWave.removeAnimationFor("scaleX");
                mUnlockWave.removeAnimationFor("scaleY");
                mUnlockWave.removeAnimationFor("alpha");
                mUnlockWave.setX(mLockCenterX + mUnlockpos);
                mUnlockWave.setY(mLockCenterY);
                mUnlockWave.setScaleY(1.0f);
                mUnlockWave.setAlpha(1.0f);
                mUnlockWave.addAnimTo(DURATION, 0, "scaleX", 0.0f, true);

                mUnlockHalo.removeAnimationFor("x");
                mUnlockHalo.removeAnimationFor("y");
                mUnlockHalo.removeAnimationFor("scaleX");
                mUnlockHalo.removeAnimationFor("scaleY");
                mUnlockHalo.removeAnimationFor("alpha");
                mUnlockHalo.setX(mLockCenterX);
                mUnlockHalo.setY(mLockCenterY);
                mUnlockHalo.setScaleX(1.0f);
                mUnlockHalo.setScaleY(1.0f);
                mUnlockHalo.setAlpha(1.0f);

                removeCallbacks(mLockTimerActions);

                mLockState = STATE_READY;
                break;

            case STATE_READY:
                break;

            case STATE_START_ATTEMPT:

                mLockState = STATE_ATTEMPTING;
                break;

            case STATE_ATTEMPTING:
                if (dragDistance > mSnapRadius) {
                    if (fingerDown) {
                        mUnlockDefault.setScaleX(1.0f);
                        mUnlockWave.setScaleX(1.0f);
                        mUnlockRing.setScaleX(1.0f);
                    }  else {
                        mLockState = STATE_UNLOCK_ATTEMPT;
                    }
                } else {
                        mUnlockDefault.setScaleX(shadescale);
                        mUnlockWave.setScaleX(shadescale);
                        mUnlockRing.setScaleX(shadescale);
                }
                break;

            case STATE_UNLOCK_ATTEMPT:
                if (dragDistance > mSnapRadius) {
                    mUnlockRing.addAnimTo(FINAL_DURATION, 0, "scaleX", 0.0f, false);
                    mUnlockWave.addAnimTo(FINAL_DURATION, 0, "scaleX", 0.0f, false);
                    mUnlockDefault.addAnimTo(FINAL_DURATION, 0, "scaleX", 0.0f, false);

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
