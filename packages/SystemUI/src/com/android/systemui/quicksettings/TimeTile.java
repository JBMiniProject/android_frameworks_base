package com.android.systemui.quicksettings;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.QuickSettingsContainerView;
import com.android.systemui.statusbar.phone.QuickSettingsController;

public class TimeTile extends QuickSettingsTile {

    public TimeTile(Context context, QuickSettingsController qsc) {
        super(context, qsc, R.layout.quick_settings_tile_time);

        mOnClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setClassName("com.android.deskclock", "com.android.deskclock.DeskClock");
                startSettingsActivity(intent);
            }
        };
    }

    @Override
    void updateQuickSettings(){}
}
