package com.android.systemui.quicksettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.systemui.R;
import com.android.systemui.statusbar.jbmpcustom.UserHelper;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.phone.QuickSettingsController;

public class UserTile extends QuickSettingsTile {

    private static final String TAG = "UserTile";
    private Drawable userAvatar;

    public UserTile(Context context, QuickSettingsController qsc) {
        super(context, qsc, R.layout.quick_settings_tile_user);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryForUserInformation();
            }
        };
        mOnLongClick = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        };
        qsc.registerAction(Intent.ACTION_CONFIGURATION_CHANGED, this);
        qsc.registerObservedContent(Settings.System.getUriFor(Settings.System.USER_MY_NUMBERS)
                , this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        queryForUserInformation();
    }

    @Override
    void onPostCreate() {
        queryForUserInformation();
        super.onPostCreate();
    }

    @Override
    public void updateResources() {
        queryForUserInformation();
    }

    @Override
    void updateQuickSettings() {
        ImageView iv = (ImageView) mTile.findViewById(R.id.user_imageview);
        TextView tv = (TextView) mTile.findViewById(R.id.user_textview);
        tv.setText(mLabel);
        iv.setImageDrawable(userAvatar);
    }

    private void queryForUserInformation() {
        ContentResolver resolver = mContext.getContentResolver();
        String numbers = Settings.System.getString(resolver, Settings.System.USER_MY_NUMBERS);
        Drawable avatar = null;
        if (numbers != null) {
            String name = UserHelper.getName(mContext, numbers);
            Bitmap rawAvatar = UserHelper.getContactPicture(mContext, numbers);
            if (rawAvatar != null) {
                avatar = new BitmapDrawable(mContext.getResources(), rawAvatar);
            } else {
                avatar = mContext.getResources().getDrawable(R.drawable.ic_qs_default_user);
            }
            if (name != null) {
                setUserTileInfo(name, avatar);
            } else {
                String names = mContext.getString(R.string.quick_settings_user_label);
                setUserTileInfo(names, avatar);
            }
        } else {
            String named = mContext.getString(R.string.quick_settings_user_label);
            avatar = mContext.getResources().getDrawable(R.drawable.ic_qs_default_user);
            setUserTileInfo(named, avatar);
        }
    }

    void setUserTileInfo(String name, Drawable avatar) {
        mLabel = name;
        userAvatar = avatar;
        updateQuickSettings();
    }

    @Override
    public void onChangeUri(ContentResolver resolver, Uri uri) {
        queryForUserInformation();
    }

}
