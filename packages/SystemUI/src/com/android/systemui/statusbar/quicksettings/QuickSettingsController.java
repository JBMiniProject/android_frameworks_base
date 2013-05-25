/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.systemui.statusbar.quicksettings;

import static com.android.internal.util.jbminiproject.QSConstants.TILES_DEFAULT;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_AIRPLANE;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_AUTOROTATE;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_BATTERY;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_BLUETOOTH;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_BRIGHTNESS;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_DELIMITER;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_EXPANDEDDESKTOP;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_GPS;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_LOCKSCREEN;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_LTE;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_MOBILEDATA;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_NETWORKMODE;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_NFC;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_PROFILE;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_QUIETHOURS;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_RINGER;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_SCREENTIMEOUT;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_SETTINGS;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_SLEEP;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_SYNC;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_TIME;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_TORCH;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_USER;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_VOLUME;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_WIFI;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_WIFIAP;
import static com.android.internal.util.jbminiproject.QSUtils.deviceSupportsBluetooth;
import static com.android.internal.util.jbminiproject.QSUtils.deviceSupportsImeSwitcher;
import static com.android.internal.util.jbminiproject.QSUtils.deviceSupportsLte;
import static com.android.internal.util.jbminiproject.QSUtils.deviceSupportsMobileData;
import static com.android.internal.util.jbminiproject.QSUtils.deviceSupportsUsbTether;
import static com.android.internal.util.jbminiproject.QSUtils.systemProfilesEnabled;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;

import com.android.systemui.statusbar.quicksettings.quicktile.AirplaneModeTile;
import com.android.systemui.statusbar.quicksettings.quicktile.AlarmTile;
import com.android.systemui.statusbar.quicksettings.quicktile.AutoRotateTile;
import com.android.systemui.statusbar.quicksettings.quicktile.BatteryTile;
import com.android.systemui.statusbar.quicksettings.quicktile.BluetoothTile;
import com.android.systemui.statusbar.quicksettings.quicktile.BrightnessTile;
import com.android.systemui.statusbar.quicksettings.quicktile.ExpandedDesktopTile;
import com.android.systemui.statusbar.quicksettings.quicktile.GPSTile;
import com.android.systemui.statusbar.quicksettings.quicktile.InputMethodTile;
import com.android.systemui.statusbar.quicksettings.quicktile.LteTile;
import com.android.systemui.statusbar.quicksettings.quicktile.MobileNetworkTile;
import com.android.systemui.statusbar.quicksettings.quicktile.MobileNetworkTypeTile;
import com.android.systemui.statusbar.quicksettings.quicktile.NfcTile;
import com.android.systemui.statusbar.quicksettings.quicktile.PreferencesTile;
import com.android.systemui.statusbar.quicksettings.quicktile.ProfileTile;
import com.android.systemui.statusbar.quicksettings.quicktile.QuickSettingsTile;
import com.android.systemui.statusbar.quicksettings.quicktile.QuietHoursTile;
import com.android.systemui.statusbar.quicksettings.quicktile.RingerModeTile;
import com.android.systemui.statusbar.quicksettings.quicktile.ScreenTimeoutTile;
import com.android.systemui.statusbar.quicksettings.quicktile.SleepScreenTile;
import com.android.systemui.statusbar.quicksettings.quicktile.SyncTile;
import com.android.systemui.statusbar.quicksettings.quicktile.TimeTile;
import com.android.systemui.statusbar.quicksettings.quicktile.ToggleLockscreenTile;
import com.android.systemui.statusbar.quicksettings.quicktile.TorchTile;
import com.android.systemui.statusbar.quicksettings.quicktile.UsbTetherTile;
import com.android.systemui.statusbar.quicksettings.quicktile.UserTile;
import com.android.systemui.statusbar.quicksettings.quicktile.VolumeTile;
import com.android.systemui.statusbar.quicksettings.quicktile.WiFiTile;
import com.android.systemui.statusbar.quicksettings.quicktile.WifiAPTile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class QuickSettingsController {
    private static String TAG = "QuickSettingsController";

    // Stores the broadcast receivers and content observers
    // quick tiles register for.
    public HashMap<String, ArrayList<QuickSettingsTile>> mReceiverMap
        = new HashMap<String, ArrayList<QuickSettingsTile>>();
    public HashMap<Uri, ArrayList<QuickSettingsTile>> mObserverMap
        = new HashMap<Uri, ArrayList<QuickSettingsTile>>();

    // Uris that need to be monitored for updating tile status
    private HashSet<Uri> mTileStatusUris = new HashSet<Uri>();

    private final Context mContext;
    private ArrayList<QuickSettingsTile> mQuickSettingsTiles;
    private final QuickSettingsContainerView mContainerView;
    private final Handler mHandler;
    private BroadcastReceiver mReceiver;
    private ContentObserver mObserver;

    private InputMethodTile mIMETile;

    private static final int MSG_UPDATE_TILES = 1000;

    public QuickSettingsController(Context context, QuickSettingsContainerView container) {
        mContext = context;
        mContainerView = container;
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case MSG_UPDATE_TILES:
                        setupQuickSettings();
                        break;
                }
            }
        };
        mQuickSettingsTiles = new ArrayList<QuickSettingsTile>();
    }

    void loadTiles() {
        // Reset reference tiles
        mIMETile = null;

        // Filter items not compatible with device
        boolean bluetoothSupported = deviceSupportsBluetooth();
        boolean mobileDataSupported = deviceSupportsMobileData(mContext);
        boolean lteSupported = deviceSupportsLte(mContext);

        if (!bluetoothSupported) {
            TILES_DEFAULT.remove(TILE_BLUETOOTH);
        }

        if (!mobileDataSupported) {
            TILES_DEFAULT.remove(TILE_WIFIAP);
            TILES_DEFAULT.remove(TILE_MOBILEDATA);
            TILES_DEFAULT.remove(TILE_NETWORKMODE);
        }

        if (!lteSupported) {
            TILES_DEFAULT.remove(TILE_LTE);
        }

        // Read the stored list of tiles
        ContentResolver resolver = mContext.getContentResolver();
        LayoutInflater inflater = LayoutInflater.from(mContext);
        String tiles = Settings.System.getString(resolver, Settings.System.QUICK_SETTINGS_TILES);
        if (tiles == null) {
            Log.i(TAG, "Default tiles being loaded");
            tiles = TextUtils.join(TILE_DELIMITER, TILES_DEFAULT);
        }

        Log.i(TAG, "Tiles list: " + tiles);

        // Split out the tile names and add to the list
        for (String tile : tiles.split("\\|")) {
            QuickSettingsTile qs = null;
            if (tile.equals(TILE_USER)) {
                qs = new UserTile(mContext, this);
            } else if (tile.equals(TILE_BATTERY)) {
                qs = new BatteryTile(mContext, this);
            } else if (tile.equals(TILE_SETTINGS)) {
                qs = new PreferencesTile(mContext, this);
            } else if (tile.equals(TILE_WIFI)) {
                qs = new WiFiTile(mContext, this);
            } else if (tile.equals(TILE_GPS)) {
                qs = new GPSTile(mContext, this);
            } else if (tile.equals(TILE_BLUETOOTH) && bluetoothSupported) {
                qs = new BluetoothTile(mContext, this);
            } else if (tile.equals(TILE_BRIGHTNESS)) {
                qs = new BrightnessTile(mContext, this, mHandler);
            } else if (tile.equals(TILE_RINGER)) {
                qs = new RingerModeTile(mContext, this);
            } else if (tile.equals(TILE_SYNC)) {
                qs = new SyncTile(mContext, this);
            } else if (tile.equals(TILE_WIFIAP) && mobileDataSupported) {
                qs = new WifiAPTile(mContext, this);
            } else if (tile.equals(TILE_SCREENTIMEOUT)) {
                qs = new ScreenTimeoutTile(mContext, this);
            } else if (tile.equals(TILE_MOBILEDATA) && mobileDataSupported) {
                qs = new MobileNetworkTile(mContext, this);
            } else if (tile.equals(TILE_LOCKSCREEN)) {
                qs = new ToggleLockscreenTile(mContext, this);
            } else if (tile.equals(TILE_NETWORKMODE) && mobileDataSupported) {
                qs = new MobileNetworkTypeTile(mContext, this);
            } else if (tile.equals(TILE_AUTOROTATE)) {
                qs = new AutoRotateTile(mContext, this, mHandler);
            } else if (tile.equals(TILE_AIRPLANE)) {
                qs = new AirplaneModeTile(mContext, this);
            } else if (tile.equals(TILE_TORCH)) {
                qs = new TorchTile(mContext, this, mHandler);
            } else if (tile.equals(TILE_SLEEP)) {
                qs = new SleepScreenTile(mContext, this);
            } else if (tile.equals(TILE_PROFILE)) {
                mTileStatusUris.add(Settings.System.getUriFor(Settings.System.SYSTEM_PROFILES_ENABLED));
                if (systemProfilesEnabled(resolver)) {
                    qs = new ProfileTile(mContext, this);
                }
            } else if (tile.equals(TILE_NFC)) {
                // User cannot add the NFC tile if the device does not support it
                // No need to check again here
                qs = new NfcTile(mContext, this);
            } else if (tile.equals(TILE_LTE)) {
                qs = new LteTile(mContext, this);
            } else if (tile.equals(TILE_TIME)) {
                qs = new TimeTile(mContext, this);
            } else if (tile.equals(TILE_QUIETHOURS)) {
                qs = new QuietHoursTile(mContext, this);
            } else if (tile.equals(TILE_VOLUME)) {
                qs = new VolumeTile(mContext, this, mHandler);
            } else if (tile.equals(TILE_EXPANDEDDESKTOP)) {
                qs = new ExpandedDesktopTile(mContext, this, mHandler);
            }

            if (qs != null) {
                qs.setupQuickSettingsTile(inflater, mContainerView);
                mQuickSettingsTiles.add(qs);
            }
        }

        // Load the dynamic tiles
        // These toggles must be the last ones added to the view, as they will show
        // only when they are needed
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_ALARM, 1) == 1) {
            QuickSettingsTile qs = new AlarmTile(mContext, this, mHandler);
            qs.setupQuickSettingsTile(inflater, mContainerView);
            mQuickSettingsTiles.add(qs);
        }
        if (deviceSupportsImeSwitcher(mContext) && Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_IME, 1) == 1) {
            mIMETile = new InputMethodTile(mContext, this);
            mIMETile.setupQuickSettingsTile(inflater, mContainerView);
            mQuickSettingsTiles.add(mIMETile);
        }
        if (deviceSupportsUsbTether(mContext) && Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_USBTETHER, 1) == 1) {
            QuickSettingsTile qs = new UsbTetherTile(mContext, this);
            qs.setupQuickSettingsTile(inflater, mContainerView);
            mQuickSettingsTiles.add(qs);
        }
    }

    public void setupQuickSettings() {
        mQuickSettingsTiles.clear();
        mContainerView.removeAllViews();
        // Clear out old receiver
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
        }
        mReceiver = new QSBroadcastReceiver();
        mReceiverMap.clear();
        ContentResolver resolver = mContext.getContentResolver();
        // Clear out old observer
        if (mObserver != null) {
            resolver.unregisterContentObserver(mObserver);
        }
        mObserver = new QuickSettingsObserver(mHandler);
        mObserverMap.clear();
        mTileStatusUris.clear();
        loadTiles();
        setupBroadcastReceiver();
        setupContentObserver();
    }

    void setupContentObserver() {
        ContentResolver resolver = mContext.getContentResolver();
        for (Uri uri : mObserverMap.keySet()) {
            resolver.registerContentObserver(uri, false, mObserver);
        }
        for (Uri uri : mTileStatusUris) {
            resolver.registerContentObserver(uri, false, mObserver);
        }
    }

    private class QuickSettingsObserver extends ContentObserver {
        public QuickSettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (mTileStatusUris.contains(uri)) {
                mHandler.removeMessages(MSG_UPDATE_TILES);
                mHandler.sendEmptyMessage(MSG_UPDATE_TILES);
            } else {
                ContentResolver resolver = mContext.getContentResolver();
                for (QuickSettingsTile tile : mObserverMap.get(uri)) {
                    tile.onChangeUri(resolver, uri);
                }
            }
        }
    }

    void setupBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        for (String action : mReceiverMap.keySet()) {
            filter.addAction(action);
        }
        mContext.registerReceiver(mReceiver, filter);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void registerInMap(Object item, QuickSettingsTile tile, HashMap map) {
        if (map.keySet().contains(item)) {
            ArrayList list = (ArrayList) map.get(item);
            if (!list.contains(tile)) {
                list.add(tile);
            }
        } else {
            ArrayList<QuickSettingsTile> list = new ArrayList<QuickSettingsTile>();
            list.add(tile);
            map.put(item, list);
        }
    }

    public void registerAction(Object action, QuickSettingsTile tile) {
        registerInMap(action, tile, mReceiverMap);
    }

    public void registerObservedContent(Uri uri, QuickSettingsTile tile) {
        registerInMap(uri, tile, mObserverMap);
    }

    private class QSBroadcastReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                for (QuickSettingsTile t : mReceiverMap.get(action)) {
                    t.onReceive(context, intent);
                }
            }
        }
    };

    public void setImeWindowStatus(boolean visible) {
        if (mIMETile != null) {
            mIMETile.toggleVisibility(visible);
        }
    }

    public void updateResources() {
        mContainerView.updateResources();
        for (QuickSettingsTile t : mQuickSettingsTiles) {
            t.updateResources();
        }
/*
        mContainerView.removeAllViews();
        setupQuickSettings();
        mContainerView.requestLayout();
*/
    }
}
