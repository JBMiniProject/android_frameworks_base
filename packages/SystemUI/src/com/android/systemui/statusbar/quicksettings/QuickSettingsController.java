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
import static com.android.internal.util.jbminiproject.QSConstants.TILE_GPS;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_LOCKSCREEN;
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
import static com.android.internal.util.jbminiproject.QSConstants.TILE_WIFI;
import static com.android.internal.util.jbminiproject.QSConstants.TILE_WIFIAP;
import static com.android.internal.util.jbminiproject.QSUtils.deviceSupportsBluetooth;
import static com.android.internal.util.jbminiproject.QSUtils.deviceSupportsTelephony;
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
import com.android.systemui.statusbar.quicksettings.quicktile.GPSTile;
import com.android.systemui.statusbar.quicksettings.quicktile.InputMethodTile;
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
import com.android.systemui.statusbar.quicksettings.quicktile.WiFiTile;
import com.android.systemui.statusbar.quicksettings.quicktile.WifiAPTile;

import java.util.ArrayList;
import java.util.HashMap;

public class QuickSettingsController {
    private static String TAG = "QuickSettingsController";

    // Stores the broadcast receivers and content observers
    // quick tiles register for.
    public HashMap<String, ArrayList<QuickSettingsTile>> mReceiverMap
        = new HashMap<String, ArrayList<QuickSettingsTile>>();
    public HashMap<Uri, ArrayList<QuickSettingsTile>> mObserverMap
        = new HashMap<Uri, ArrayList<QuickSettingsTile>>();

    private final Context mContext;
    private ArrayList<QuickSettingsTile> mQuickSettingsTiles;
    private final QuickSettingsContainerView mContainerView;
    private final Handler mHandler;
    private BroadcastReceiver mReceiver;
    private ContentObserver mObserver;

    private InputMethodTile IMETile;

    public QuickSettingsController(Context context, QuickSettingsContainerView container) {
        mContext = context;
        mContainerView = container;
        mHandler = new Handler();
        mQuickSettingsTiles = new ArrayList<QuickSettingsTile>();
    }

    void loadTiles() {
        // Filter items not compatible with device
        boolean bluetoothSupported = deviceSupportsBluetooth();
        boolean telephonySupported = deviceSupportsTelephony(mContext);

        if (!bluetoothSupported) {
            TILES_DEFAULT.remove(TILE_BLUETOOTH);
        }

        if (!telephonySupported) {
            TILES_DEFAULT.remove(TILE_WIFIAP);
            TILES_DEFAULT.remove(TILE_MOBILEDATA);
            TILES_DEFAULT.remove(TILE_NETWORKMODE);
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
                qs = new UserTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_BATTERY)) {
                qs = new BatteryTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_SETTINGS)) {
                qs = new PreferencesTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_WIFI)) {
                qs = new WiFiTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_GPS)) {
                qs = new GPSTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_BLUETOOTH) && bluetoothSupported) {
                qs = new BluetoothTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_BRIGHTNESS)) {
                qs = new BrightnessTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_RINGER)) {
                qs = new RingerModeTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_SYNC)) {
                qs = new SyncTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_WIFIAP) && telephonySupported) {
                qs = new WifiAPTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_SCREENTIMEOUT)) {
                qs = new ScreenTimeoutTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_MOBILEDATA) && telephonySupported) {
                qs = new MobileNetworkTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_LOCKSCREEN)) {
                qs = new ToggleLockscreenTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_NETWORKMODE) && telephonySupported) {
                qs = new MobileNetworkTypeTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_AUTOROTATE)) {
                qs = new AutoRotateTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_AIRPLANE)) {
                qs = new AirplaneModeTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_TORCH)) {
                qs = new TorchTile(mContext, inflater, mContainerView, this, mHandler);
            } else if (tile.equals(TILE_SLEEP)) {
                qs = new SleepScreenTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_PROFILE) && systemProfilesEnabled(resolver)) {
                qs = new ProfileTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_NFC)) {
                // User cannot add the NFC tile if the device does not support it
                // No need to check again here
                qs = new NfcTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_TIME)) {
                qs = new TimeTile(mContext, inflater, mContainerView, this);
            } else if (tile.equals(TILE_QUIETHOURS)) {
                qs = new QuietHoursTile(mContext, inflater, mContainerView, this);
            }
            if (qs != null) {
                qs.setupQuickSettingsTile();
                mQuickSettingsTiles.add(qs);
            }
        }

        // Load the dynamic tiles
        // These toggles must be the last ones added to the view, as they will show
        // only when they are needed
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_ALARM, 1) == 1) {
            QuickSettingsTile qs = new AlarmTile(mContext, inflater, mContainerView, this);
            qs.setupQuickSettingsTile();
            mQuickSettingsTiles.add(qs);
        }
        if (Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_IME, 1) == 1) {
            QuickSettingsTile qs = new InputMethodTile(mContext, inflater, mContainerView, this);
            qs.setupQuickSettingsTile();
            mQuickSettingsTiles.add(qs);
        }
        if (deviceSupportsUsbTether(mContext) && Settings.System.getInt(resolver, Settings.System.QS_DYNAMIC_USBTETHER, 1) == 1) {
            QuickSettingsTile qs = new UsbTetherTile(mContext, inflater, mContainerView, this);
            qs.setupQuickSettingsTile();
            mQuickSettingsTiles.add(qs);
        }
    }

    private void setupQuickSettings() {
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
        loadTiles();
        setupBroadcastReceiver();
        setupContentObserver();
    }

    void setupContentObserver() {
        ContentResolver resolver = mContext.getContentResolver();
        for (Uri uri : mObserverMap.keySet()) {
            resolver.registerContentObserver(uri, false, mObserver);
        }
    }

    private class QuickSettingsObserver extends ContentObserver {
        public QuickSettingsObserver(Handler handler) {
            super(handler);
        }

        public void onChange(boolean selfChange, Uri uri) {
            ContentResolver resolver = mContext.getContentResolver();
            for (QuickSettingsTile tile : mObserverMap.get(uri)) {
                tile.onChangeUri(resolver, uri);
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
        if (IMETile != null) {
            IMETile.toggleVisibility(visible);
        }
    }

    public void updateResources() {
        mContainerView.updateResources();
        mContainerView.removeAllViews();
        setupQuickSettings();
        mContainerView.requestLayout();
    }
}
