package com.upbad.apps.autonotify;

import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

// Source: https://stackoverflow.com/questions/39320048/how-to-detect-if-phone-is-connected-to-android-auto/75292070#75292070
public class AutoConnectionDetector {
   private final Context context;
   private static String TAG = "AutoConnectionDetector";
   private final CarConnectionBroadcastReceiver carConnectionReceiver = new CarConnectionBroadcastReceiver();
   private final CarConnectionQueryHandler carConnectionQueryHandler;
   // columnName for provider to query on connection status
   private static final String CAR_CONNECTION_STATE = "CarConnectionState";

   // auto app on your phone will send broadcast with this action when connection state changes
   private final String ACTION_CAR_CONNECTION_UPDATED = "androidx.car.app.connection.action.CAR_CONNECTION_UPDATED";

   // phone is not connected to car
   private static final int CONNECTION_TYPE_NOT_CONNECTED = 0;

   // phone is connected to Automotive OS
   private final int CONNECTION_TYPE_NATIVE = 1;

   // phone is connected to Android Auto
   private final int CONNECTION_TYPE_PROJECTION = 2;

   private final int QUERY_TOKEN = 42;

   private final String CAR_CONNECTION_AUTHORITY = "androidx.car.app.connection";

   private final Uri PROJECTION_HOST_URI = new Uri.Builder().scheme("content").authority(CAR_CONNECTION_AUTHORITY).build();

   public interface OnCarConnectionStateListener {
      void onCarConnected();

      void onCarDisconnected();
   }

   private static OnCarConnectionStateListener listener;

   public void setListener(OnCarConnectionStateListener listener) {
      AutoConnectionDetector.listener = listener;
   }

   public AutoConnectionDetector(Context context) {
      this.context = context;
      carConnectionQueryHandler = new CarConnectionQueryHandler(context.getContentResolver());
   }

   public void registerCarConnectionReceiver() {
      context.registerReceiver(carConnectionReceiver, new IntentFilter(ACTION_CAR_CONNECTION_UPDATED));
      queryForState();
   }

   public void unRegisterCarConnectionReceiver() {
      context.unregisterReceiver(carConnectionReceiver);
   }

   private void queryForState() {
      String[] projection = {CAR_CONNECTION_STATE};
      carConnectionQueryHandler.startQuery(
              QUERY_TOKEN,
              null,
              PROJECTION_HOST_URI,
              projection,
              null,
              null,
              null
      );
   }

   private static void notifyCarConnected() {
      listener.onCarConnected();
   }

   private static void notifyCarDisconnected() {
      listener.onCarDisconnected();
   }

   class CarConnectionBroadcastReceiver extends BroadcastReceiver {
      // query for connection state every time the receiver receives the broadcast
      @Override
      public void onReceive(Context context, Intent intent) {
         queryForState();
      }
   }

   private static class CarConnectionQueryHandler extends AsyncQueryHandler {
      public CarConnectionQueryHandler(ContentResolver contentResolver) {
         super(contentResolver);
      }

      @Override
      protected void onQueryComplete(int token, Object cookie, Cursor response) {
         if (response == null) {
            // Null response from content provider when checking connection to the car, treating as disconnected
            notifyCarDisconnected();
            return;
         }
         int carConnectionTypeColumn = response.getColumnIndex(CAR_CONNECTION_STATE);
         if (carConnectionTypeColumn < 0) {
            // Connection to car response is missing the connection type, treating as disconnected
            notifyCarDisconnected();
         } else if (! response.moveToNext()) {
            // Connection to car response is empty, treating as disconnected
            notifyCarDisconnected();
         } else {
            int connectionState = response.getInt(carConnectionTypeColumn);
            if (connectionState == CONNECTION_TYPE_NOT_CONNECTED) {
               notifyCarDisconnected();
            } else {
               notifyCarConnected();
            }
         }
         response.close();
      }
   }
}
