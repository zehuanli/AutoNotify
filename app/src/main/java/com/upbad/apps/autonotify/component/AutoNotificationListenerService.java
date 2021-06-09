package com.upbad.apps.autonotify.component;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;

import com.upbad.apps.autonotify.R;
import com.upbad.apps.autonotify.Util;
import com.upbad.apps.autonotify.db.AppDatabase;

public class AutoNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "AutoNotificationListenerService";

    private AppDatabase db;

    public AutoNotificationListenerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.hasExtra(MessagingIntentService.NOTIFICATION_KEY_EXTRA)) {
            String notificationKey = intent.getStringExtra(MessagingIntentService.NOTIFICATION_KEY_EXTRA);
            cancelNotification(notificationKey);
        }
        return START_STICKY;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        // Check if the phone is in car mode (not necessarily connected to Android Auto)
        if (((UiModeManager) getSystemService(Context.UI_MODE_SERVICE)).getCurrentModeType() != Configuration.UI_MODE_TYPE_CAR) {
            return;
        }

        String packageName = sbn.getPackageName();
        // Cancel the notification by this app itself
        if (packageName.equals(this.getPackageName())) {
            cancelNotification(sbn.getKey());
            return;
        }

        int userId = Util.getUserIdFromUserHandle(sbn.getUser());

        if (getDB().packageDAO().find(sbn.getPackageName(), userId) != null) {
            // Obtain basics
            Bundle notificationExtras = sbn.getNotification().extras;
            int notificationId = sbn.getId();
            String notificationKey = sbn.getKey();
            String label = Util.getLabelFromPackageName(this, packageName);
            Drawable iconDrawable = Util.getIconFromPackageName(this, packageName);
            Bitmap iconBitmap = Util.drawableToBitmap(iconDrawable);
            IconCompat iconCompat = null;
            try {
                iconCompat = IconCompat.createWithBitmap(iconBitmap);
            } catch (Exception ignored) {
            }
            if (iconCompat == null) {
                iconCompat = IconCompat.createWithResource(this, R.drawable.ic_launcher_foreground);
            }
            // Create Person
            Person person =
                    new Person.Builder()
                            .setName(label)
                            .setIcon(iconCompat)
                            .build();
            // Create MessagingStyle
            NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(person);
            messagingStyle.setConversationTitle(label);
            messagingStyle.setGroupConversation(false);
            messagingStyle.addMessage(notificationExtras.getString(Notification.EXTRA_TEXT), sbn.getPostTime(), person);
            // Create Reply PendingIntent (not used)
            Intent replyIntent = new Intent(this, MessagingIntentService.class);
            replyIntent.setAction(MessagingIntentService.ACTION_REPLY);
            PendingIntent replyPendingIntent = PendingIntent.getService(this, notificationId, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            // Create Reply Action (not used)
            NotificationCompat.Action replyAction =
                    new NotificationCompat.Action.Builder(R.drawable.ic_baseline_check_24, "Reply", replyPendingIntent)
                            .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                            .setShowsUserInterface(false)
                            .addRemoteInput(new RemoteInput.Builder("N/A").build())
                            .build();
            // Create Mark-as-Read PendingIntent
            Intent markAsReadIntent = new Intent(this, MessagingIntentService.class);
            markAsReadIntent.setAction(MessagingIntentService.ACTION_MARK_AS_READ);
            markAsReadIntent.putExtra(MessagingIntentService.NOTIFICATION_KEY_EXTRA, notificationKey);
            PendingIntent markAsReadPendingIntent = PendingIntent.getService(this, notificationId, markAsReadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            // Create Mark-as-Read Action
            NotificationCompat.Action markAsReadAction =
                    new NotificationCompat.Action.Builder(R.drawable.ic_baseline_check_24, "Mark as Read", markAsReadPendingIntent)
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                    .setShowsUserInterface(false)
                    .build();
            // Create Notification
            Notification notification =
                    new NotificationCompat.Builder(this, getString(R.string.channel_id))
                            .setSmallIcon(iconCompat)
                            .setLargeIcon(iconBitmap)
                            .setStyle(messagingStyle)
                            .addInvisibleAction(replyAction)
                            .addAction(markAsReadAction)
                            .build();
            NotificationManagerCompat.from(this).notify(notificationId, notification);
        }
    }

    private AppDatabase getDB() {
        if (db == null || !db.isOpen()) {
            db = Util.getDatabase(this);
        }
        return db;
    }
}