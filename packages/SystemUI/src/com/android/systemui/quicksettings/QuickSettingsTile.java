package com.android.systemui.quicksettings;

import com.android.internal.statusbar.IStatusBarService;
import android.app.ActivityManagerNative;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsController;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.phone.QuickSettingsTileView;

public class QuickSettingsTile implements OnClickListener {

    protected final Context mContext;
    protected QuickSettingsTileView mTile;
    protected OnClickListener mOnClick;
    protected OnLongClickListener mOnLongClick;
    protected final int mTileLayout;
    protected int mDrawable;
    protected String mLabel;
    IStatusBarService mStatusBarService;
    Handler mHandler;
    protected QuickSettingsController mQsc;


    public QuickSettingsTile(Context context, QuickSettingsController qsc) {
        this(context, qsc, R.layout.quick_settings_tile_generic);
    }

    public QuickSettingsTile(Context context, QuickSettingsController qsc, int layout) {
        mContext = context;
        mDrawable = R.drawable.ic_notifications;
        mLabel = mContext.getString(R.string.quick_settings_label_enabled);
        mQsc = qsc;
        mHandler = new Handler();
        mTileLayout = layout;
    }

    public void setupQuickSettingsTile(LayoutInflater inflater, QuickSettingsContainerView container) {
        mTile = (QuickSettingsTileView) inflater.inflate(R.layout.quick_settings_tile, container, false);
        mTile.setContent(mTileLayout, inflater);
        container.addView(mTile);
        onPostCreate();
        updateQuickSettings();
        mTile.setOnClickListener(this);
        mTile.setOnLongClickListener(mOnLongClick);
    }

    void onPostCreate(){}

    public void onDestroy() {}

    public void onReceive(Context context, Intent intent) {}

    public void onChangeUri(ContentResolver resolver, Uri uri) {}

    public void updateResources() {
        if(mTile != null) {
            updateQuickSettings();
        }
    }

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
        try {
            // Dismiss the lock screen when Settings starts.
            ActivityManagerNative.getDefault().dismissKeyguardOnNextActivity();
        } catch (RemoteException e) {
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mContext.startActivity(intent);
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

    @Override
    public final void onClick(View v) {
        mOnClick.onClick(v);
        ContentResolver resolver = mContext.getContentResolver();
        boolean shouldCollapse = Settings.System.getInt(resolver, Settings.System.QS_COLLAPSE_PANEL, 0) == 1;
        if (shouldCollapse) {
            startCollapseActivity();
        }
    }

}
