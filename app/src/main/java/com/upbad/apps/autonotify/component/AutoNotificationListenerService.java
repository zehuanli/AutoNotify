package com.upbad.apps.autonotify.component;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.upbad.apps.autonotify.Util;
import com.upbad.apps.autonotify.db.AppDatabase;

public class AutoNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "autonotify";

    private AppDatabase db;
    private boolean carConnected = true;

    public AutoNotificationListenerService() {
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (carConnected) {
            int userId = Util.getUserIdFromUserHandle(sbn.getUser());
            //// Test
            Bundle _notificationExtras = sbn.getNotification().extras;
            Log.d(TAG, "onNotificationPosted: Label: " + Util.getLabelFromPackageName(this, sbn.getPackageName()));
            Log.d(TAG, "onNotificationPosted: UserId: " + userId);
            Log.d(TAG, "onNotificationPosted: Title: " + _notificationExtras.getString(Notification.EXTRA_TITLE));
            Log.d(TAG, "onNotificationPosted: Text: " + _notificationExtras.getString(Notification.EXTRA_TEXT));
            ////
            if (getDB().packageDAO().find(sbn.getPackageName(), userId) != null) {
                // TODO: Notify Android Auto
                Bundle notificationExtras = sbn.getNotification().extras;
                cancelNotification(sbn.getKey()); // For testing
            }
        }
    }

    private AppDatabase getDB() {
        if (db == null || !db.isOpen()) {
            db = Util.getDatabase(this);
        }
        return db;
    }
}