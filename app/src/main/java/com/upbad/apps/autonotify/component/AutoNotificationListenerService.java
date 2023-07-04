package com.upbad.apps.autonotify.component;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;
import androidx.core.app.RemoteInput;
import androidx.core.graphics.drawable.IconCompat;

import com.upbad.apps.autonotify.AutoConnectionDetector;
import com.upbad.apps.autonotify.Config;
import com.upbad.apps.autonotify.R;
import com.upbad.apps.autonotify.Util;
import com.upbad.apps.autonotify.db.AppDatabase;
import com.upbad.apps.autonotify.db.BlacklistRecord;

import java.util.List;

public class AutoNotificationListenerService extends NotificationListenerService {

    private static final String TAG = "AutoNotificationListenerService";

    private SharedPreferences pref;

    AutoConnectionDetector autoConnectionDetector;
    private boolean carConnected;

    public AutoNotificationListenerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra(MessagingIntentService.NOTIFICATION_KEY_EXTRA)) {
            String notificationKey = intent.getStringExtra(MessagingIntentService.NOTIFICATION_KEY_EXTRA);
            if (! notificationKey.isEmpty()) {
                cancelNotification(notificationKey);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onListenerConnected() {
        autoConnectionDetector = new AutoConnectionDetector(this);
        autoConnectionDetector.setListener(new AutoConnectionDetector.OnCarConnectionStateListener() {
            @Override
            public void onCarConnected() {
                carConnected = true;
            }

            @Override
            public void onCarDisconnected() {
                carConnected = false;
            }
        });
        autoConnectionDetector.registerCarConnectionReceiver();
    }

    @Override
    public void onDestroy() {
        autoConnectionDetector.unRegisterCarConnectionReceiver();
        super.onDestroy();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (pref == null) {
            pref = getSharedPreferences(Config.SHARED_PREFERENCE_STRING, MODE_PRIVATE);
        }
        if (! pref.getBoolean(Config.PREFERENCE_ENABLED, true)) {
            return;
        }

        // Check if the phone is in car mode (not necessarily connected to Android Auto)
        if (! carConnected) {
            return;
        }

        String packageName = sbn.getPackageName();
        // Cancel the notification by this app itself
        if (packageName.equals(this.getPackageName()) && sbn.getId() != -1) {
            cancelNotification(sbn.getKey());
            return;
        }

        int userId = Util.getUserIdFromUserHandle(sbn.getUser());

        if (AppDatabase.getInstance(this).packageDAO().find(sbn.getPackageName(), userId) != null) {
            // Obtain basics
            Bundle notificationExtras = sbn.getNotification().extras;
            int notificationId = sbn.getId();
            String notificationKey = sbn.getKey();
            String notificationTitle = notificationExtras.getString(Notification.EXTRA_TITLE);
            String notificationText = notificationExtras.getString(Notification.EXTRA_TEXT);
            if (isBlacklisted(notificationTitle, notificationText)) {
                return;
            }
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
            messagingStyle.setConversationTitle(notificationTitle);
            messagingStyle.setGroupConversation(false);
            messagingStyle.addMessage(notificationText, sbn.getPostTime(), person);
            // Create Reply PendingIntent (not used)
            Intent replyIntent = new Intent(this, MessagingIntentService.class);
            replyIntent.setAction(MessagingIntentService.ACTION_REPLY);
            PendingIntent replyPendingIntent = PendingIntent.getService(this, notificationId, replyIntent, PendingIntent.FLAG_IMMUTABLE);
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
            PendingIntent markAsReadPendingIntent = PendingIntent.getService(this, notificationId, markAsReadIntent, PendingIntent.FLAG_IMMUTABLE);
            // Create Mark-as-Read Action
            NotificationCompat.Action markAsReadAction =
                    new NotificationCompat.Action.Builder(R.drawable.ic_baseline_check_24, "Mark as Read", markAsReadPendingIntent)
                    .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                    .setShowsUserInterface(false)
                    .build();
            // Create Notification
            Notification notification =
                    new NotificationCompat.Builder(this, getString(R.string.channel_id))
                            .setCategory(Notification.CATEGORY_MESSAGE)
                            .setSmallIcon(iconCompat)
                            .setLargeIcon(iconBitmap)
                            .setStyle(messagingStyle)
                            .addInvisibleAction(replyAction)
                            .addAction(markAsReadAction)
                            .build();
            NotificationManagerCompat.from(this).notify(notificationId, notification);
        }
    }

    private boolean isBlacklisted(String... args) {
        AppDatabase db = AppDatabase.getInstance(this);
        List<BlacklistRecord> blacklistRecords = db.blacklistDAO().getAll();
        for (BlacklistRecord blacklistRecord : blacklistRecords) {
            for (String arg : args) {
                String argLowercase = arg.toLowerCase();
                if (argLowercase.contains(blacklistRecord.keyword.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressLint("MissingPermission")
    public static void generateTestNotification(Context context) {
        IconCompat iconCompat = IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground);
        // Create Person
        Person person =
                new Person.Builder()
                        .setName("Test app")
                        .setIcon(iconCompat)
                        .build();
        // Create MessagingStyle
        NotificationCompat.MessagingStyle messagingStyle = new NotificationCompat.MessagingStyle(person);
        messagingStyle.setConversationTitle("Test notification");
        messagingStyle.setGroupConversation(false);
        messagingStyle.addMessage("This is a test message from AutoNotify", System.currentTimeMillis(), person);
        // Create Reply PendingIntent (not used)
        Intent replyIntent = new Intent(context, MessagingIntentService.class);
        replyIntent.setAction(MessagingIntentService.ACTION_REPLY);
        PendingIntent replyPendingIntent = PendingIntent.getService(context, -1, replyIntent, PendingIntent.FLAG_IMMUTABLE);
        // Create Reply Action (not used)
        NotificationCompat.Action replyAction =
                new NotificationCompat.Action.Builder(R.drawable.ic_baseline_check_24, "Reply", replyPendingIntent)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                        .setShowsUserInterface(false)
                        .addRemoteInput(new RemoteInput.Builder("N/A").build())
                        .build();
        // Create Mark-as-Read PendingIntent
        Intent markAsReadIntent = new Intent(context, MessagingIntentService.class);
        markAsReadIntent.setAction(MessagingIntentService.ACTION_MARK_AS_READ);
        markAsReadIntent.putExtra(MessagingIntentService.NOTIFICATION_KEY_EXTRA, "");
        PendingIntent markAsReadPendingIntent = PendingIntent.getService(context, -1, markAsReadIntent, PendingIntent.FLAG_IMMUTABLE);
        // Create Mark-as-Read Action
        NotificationCompat.Action markAsReadAction =
                new NotificationCompat.Action.Builder(R.drawable.ic_baseline_check_24, "Mark as Read", markAsReadPendingIntent)
                        .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                        .setShowsUserInterface(false)
                        .build();
        // Create Notification
        Notification notification =
                new NotificationCompat.Builder(context, context.getString(R.string.channel_id))
                        .setCategory(Notification.CATEGORY_MESSAGE)
                        .setSmallIcon(iconCompat)
                        .setStyle(messagingStyle)
                        .addInvisibleAction(replyAction)
                        .addAction(markAsReadAction)
                        .build();
        NotificationManagerCompat.from(context).notify(-1, notification);
    }
}