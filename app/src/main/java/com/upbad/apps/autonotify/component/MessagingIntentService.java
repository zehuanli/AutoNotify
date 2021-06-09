package com.upbad.apps.autonotify.component;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

public class MessagingIntentService extends IntentService {

    private static final String TAG = "MessagingIntentService";

    public static final String NOTIFICATION_KEY_EXTRA = "NOTIFICATION_KEY";

    public static final String ACTION_REPLY = "com.upbad.apps.autonotify.action.REPLY";
    public static final String ACTION_MARK_AS_READ = "com.upbad.apps.autonotify.action.MARK_AS_READ";

    public MessagingIntentService() {
        super("MessagingIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            String notificationKey = intent.getStringExtra(NOTIFICATION_KEY_EXTRA);
            if (ACTION_REPLY.equals(action)) {
                // Do nothing
            } else if (ACTION_MARK_AS_READ.equals(action)) {
                handleActionMarkAsRead(notificationKey);
            }
        }
    }

    /**
     * Send the notificationKey back to AutoNotificationListenerService
     * @param notificationKey
     */
    private void handleActionMarkAsRead(String notificationKey) {
        Intent intent = new Intent(this, AutoNotificationListenerService.class);
        intent.putExtra(NOTIFICATION_KEY_EXTRA, notificationKey);
        startService(intent);
    }
}