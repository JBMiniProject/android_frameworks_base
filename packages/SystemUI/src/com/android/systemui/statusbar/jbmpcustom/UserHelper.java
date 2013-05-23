package com.android.systemui.statusbar.jbmpcustom;

import android.content.Context;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import java.io.InputStream;

public class UserHelper {
    public static String getName(Context context, String callNumber) {
        String caller = null;
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                callNumber); 
        Cursor cursor = context.getContentResolver().query(
                uri, new String[] { PhoneLookup.DISPLAY_NAME },
                null, null, null);
        String[] contacts = new String[] { PhoneLookup.DISPLAY_NAME };
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(contacts[0]);
                caller = cursor.getString(nameIndex);
            }
        }
        if (caller == null) {
            caller = callNumber;
        }
        if (cursor != null) {
            cursor.close();
        }
        return caller;
    }

    public static Bitmap getContactPicture(Context context, String callNumber) {
        Bitmap bitmap = null;
        Bitmap scaledBitmap = null;
        Cursor cursor = context.getContentResolver().query(
                Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                Uri.decode(callNumber)),
                new String[] { PhoneLookup._ID },
                null, null, null);
        if (cursor.moveToFirst()) {
            long contactId = cursor.getLong(0);
            InputStream inputStream = Contacts.openContactPhotoInputStream(
                    context.getContentResolver(),
                    ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId));
            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream);
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, 60, 60, true);
            }
        }
        if (cursor != null) {
            cursor.close();
        }
        return scaledBitmap;
    }
}
