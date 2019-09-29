package com.hasbis.findonpage.utils;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class FirebaseAnalyticsUtils {
    private final static String TAG = "FirebaseAnalyticsUtils";

    private static FirebaseAnalyticsUtils instance = null;
    private FirebaseAnalytics mFirebaseAnalytics;

    private FirebaseAnalyticsUtils()
    {
    }

    public static FirebaseAnalyticsUtils getInstance() {
        synchronized (TAG) {
            if (instance == null) {
                synchronized (TAG) {
                    instance = new FirebaseAnalyticsUtils();
                }
            }
        }
        return instance;
    }

    public void init(Context context) {
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    private void saveLog(String id, String name) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void onNewPhoto() {
        saveLog("PHOTO", "NEW_PHOTO");
    }

    public void onNewGalleryPhoto() {
        saveLog("GALLERY", "NEW_GALLERY");
    }

    public void onNewSearch() {
        saveLog("SEARCH", "NEW_SEARCH");
    }

    public void onSelectAll() {
        saveLog("SELECT_ALL", "NEW_SELECT_ALL");
    }

    public void onShowFindBoxClicked() {
        saveLog("SHOW_FIND_BOX", "NEW_SHOW_FIND_BOX");
    }
}
