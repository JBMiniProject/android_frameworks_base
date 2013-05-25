package com.android.systemui.statusbar.quicksettings.quicktile;

import com.android.internal.statusbar.IStatusBarService;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.quicksettings.QuickSettingsController;
import com.android.systemui.statusbar.quicksettings.QuickSettingsContainerView;
import com.android.systemui.statusbar.quicksettings.QuickSettingsTileView;

public class QuickSettingsTile implements OnClickListener {

    protected final Context mContext;
    protected final ViewGroup mContainerView;
    protected final LayoutInflater mInflater;
    protected QuickSettingsTileView mTile;
    protected OnClickListener mOnClick;
    protected OnLongClickListener mOnLongClick;
    protected int mTileLayout;
    protected int mDrawable;
    protected String mLabel;
    protected QuickSettingsController mQsc;
    IStatusBarService mStatusBarService;
    Handler mHandler;
    protected boolean mHapticFeedback;
    protected Vibrator mVibrator;
    private long[] mClickPattern;
    private long[] mLongClickPattern;

    public QuickSettingsTile(Context context, LayoutInflater inflater, QuickSettingsContainerView container, QuickSettingsController qsc) {
        mContext = context;
        mContainerView = container;
        mInflater = inflater;
        mDrawable = R.drawable.stat_sys_roaming_cdma_0;
        mLabel = mContext.getString(R.string.quick_settings_label_enabled);
        mQsc = qsc;
        mTileLayout = R.layout.quick_settings_tile_generic;
        mHandler = new Handler();
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public void setupQuickSettingsTile(){
        createQuickSettings();
        onPostCreate();
        updateQuickSettings();
        mTile.setOnClickListener(this);
        mTile.setOnLongClickListener(mOnLongClick);
        updateHapticFeedbackSetting();
    }

    void createQuickSettings(){
        mTile = (QuickSettingsTileView) mInflater.inflate(R.layout.quick_settings_tile, mContainerView, false);
        mTile.setContent(mTileLayout, mInflater);
        mContainerView.addView(mTile);
    }

    void onPostCreate(){}

    public void onReceive(Context context, Intent intent) {}

    public void onChangeUri(ContentResolver resolver, Uri uri) {}

    void updateQuickSettings(){
        TextView tv = (TextView) mTile.findViewById(R.id.tile_textview);
        tv.setCompoundDrawablesWithIntrinsicBounds(0, mDrawable, 0, 0);
        tv.setText(mLabel);
    }

    void startSettingsActivity(String action){
        Intent intent = new Intent(action);
        startSettingsActivity(intent);
    }

    void startSettingsActivity(Intent intent) {
        startSettingsActivity(intent, true);
    }

    void startSettingsActivity(Intent intent, boolean onlyProvisioned) {
        if (onlyProvisioned) return;
        try {
            // Dismiss the lock screen when Settings starts.
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (RemoteException e) {
        }
        updateHapticFeedbackSetting();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
        provideHapticFeedback(mLongClickPattern);
        startCollapseActivity();
    }

    void startCollapseActivity() {
        mHandler.post(new Runnable() { public void run() {
            try {
                 IStatusBarService statusbar = getStatusBarService();
                 if (statusbar != null) {
                     statusbar.collapse();
                 }
            } catch (RemoteException ex) {
                 // re-acquire status bar service next time it is needed.
                 mStatusBarService = null;
            }
        }});
    }

    IStatusBarService getStatusBarService() {
        if (mStatusBarService == null) {
            mStatusBarService = IStatusBarService.Stub.asInterface(
                    ServiceManager.getService("statusbar"));
        }
        return mStatusBarService;
    }

    private void updateHapticFeedbackSetting() {
        ContentResolver cr = mContext.getContentResolver();
        int expandedHapticFeedback = Settings.System.getInt(cr,
                Settings.System.EXPANDED_HAPTIC_FEEDBACK, 2);
        long[] clickPattern = null, longClickPattern = null;
        boolean hapticFeedback;

        if (expandedHapticFeedback == 2) {
             hapticFeedback = Settings.System.getInt(cr,
                     Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1;
        } else {
            hapticFeedback = (expandedHapticFeedback == 1);
        }

/*        if (hapticFeedback) {
            clickPattern = Settings.System.getLongArray(cr,
                    Settings.System.HAPTIC_DOWN_ARRAY, null);
            longClickPattern = Settings.System.getLongArray(cr,
                    Settings.System.HAPTIC_LONG_ARRAY, null);
        }
*/
        setHapticFeedback(hapticFeedback, clickPattern, longClickPattern);
    }

    void setHapticFeedback(boolean enabled, long[] clickPattern, long[] longClickPattern) {
        mHapticFeedback = enabled;
        mClickPattern = clickPattern;
        mLongClickPattern = longClickPattern;
    }

    @Override
    public final void onClick(View v) {
        updateHapticFeedbackSetting();
        mOnClick.onClick(v);
        ContentResolver resolver = mContext.getContentResolver();
        boolean shouldCollapse = Settings.System.getInt(resolver, Settings.System.QS_COLLAPSE_PANEL, 0) == 1;
        if (shouldCollapse) {
            startCollapseActivity();
        }
        provideHapticFeedback(mClickPattern);
    }

    private void provideHapticFeedback(long[] pattern) {
        if (mHapticFeedback && pattern != null) {
            if (pattern.length == 1) {
                mVibrator.vibrate(pattern[0]);
            } else {
                mVibrator.vibrate(pattern, -1);
            }
        }
    }

}
