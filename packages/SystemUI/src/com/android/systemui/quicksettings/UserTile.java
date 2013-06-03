package com.android.systemui.quicksettings;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.net.Uri;
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
        qsc.registerAction(Intent.ACTION_INSERT_OR_EDIT, this);
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
        String number    = "5267";
        Drawable avatar  = null;
        String name      = UserHelper.getName(mContext, number);
        Bitmap rawAvatar = UserHelper.getContactPicture(mContext, number);

        Drawable no_avatar = mContext.getResources().getDrawable(R.drawable.ic_qs_default_user);
        String no_name     = mContext.getString(R.string.quick_settings_user_label);

        if (rawAvatar != null) {
            avatar = new BitmapDrawable(mContext.getResources(), rawAvatar);
        } else {
            avatar = no_avatar;
        }

        if (name != null && !(name.equals("5267"))) {
            setUserTileInfo(name, avatar);
        } else {
            setUserTileInfo(no_name, avatar);
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
