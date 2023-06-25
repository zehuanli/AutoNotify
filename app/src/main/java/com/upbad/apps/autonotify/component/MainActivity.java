package com.upbad.apps.autonotify.component;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.upbad.apps.autonotify.R;
import com.upbad.apps.autonotify.Util;
import com.upbad.apps.autonotify.db.AppDatabase;
import com.upbad.apps.autonotify.db.PackageData;

import java.util.List;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 133;

    private Context context;
    private Button listAppButton;
    private Button enterAppInfoButton;
    SwipeRefreshLayout swipeRefreshLayout;
    private ListView currentAppListView;

    ArrayAdapter<PackageData> currentAppListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;

        // Check if the Android Auto app is installed
        boolean androidAutoInstalled = false;
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo applicationInfo;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                applicationInfo = packageManager.getApplicationInfo("com.google.android.projection.gearhead", PackageManager.ApplicationInfoFlags.of(0));
            } else {
                applicationInfo = packageManager.getApplicationInfo("com.google.android.projection.gearhead", 0);
            }
            if (applicationInfo.enabled) {
                androidAutoInstalled = true;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        if (! androidAutoInstalled) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.error)
                    .setMessage(R.string.android_auto_not_installed)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(R.string.ok, null)
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            finishAndRemoveTask();
                        }
                    })
                    .show();
        }

        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU && ! shouldShowRequestPermissionRationale("Post notification permission is required")) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
        }

        listAppButton = findViewById(R.id.listAppButton);
        enterAppInfoButton = findViewById(R.id.enterAppInfoButton);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        currentAppListView = findViewById(R.id.currentAppListView);

        listAppButton.setOnClickListener(v -> {
            findViewById(R.id.progressAnimation).setVisibility(View.VISIBLE);
            Intent startIntent = new Intent(context, AppSearchActivity.class);
            startActivityForResult(startIntent, 1);
        });

        enterAppInfoButton.setOnClickListener(view -> {
            LayoutInflater layoutInflater = LayoutInflater.from(view.getContext());
            View popupView = layoutInflater.inflate(R.layout.dialog_app_edit, null);
            TextView appPackageName = popupView.findViewById(R.id.appPackageName);
            TextView appUserIdDialog = popupView.findViewById(R.id.appUserIdDialog);
            TextView appLabel = popupView.findViewById(R.id.appLabel);

            final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
            popupWindow.setElevation(10);
            popupWindow.setFocusable(true);
            popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

            Button button = popupView.findViewById(R.id.button);
            button.setOnClickListener(v -> {
                String packageName = appPackageName.getText().toString();
                int userId = -1;
                try {
                    userId = Integer.parseInt(appUserIdDialog.getText().toString());
                } catch (Exception ignored) {
                }
                if (userId < 0 || packageName.isBlank()) {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.error)
                            .setMessage(R.string.enter_error_message)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.ok, null)
                            .show();
                    return;
                }
                String label = appLabel.getText().toString();
                AppDatabase db = Util.getDatabase(context);
                db.packageDAO().insert(new PackageData(packageName, userId, label.isBlank() ? packageName : label));
                db.close();
                popupWindow.dismiss();
                reloadAppList();
            });
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            reloadAppList();
            swipeRefreshLayout.setRefreshing(false);
        });

        currentAppListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final PackageData packageData = currentAppListAdapter.getItem(position);
                LayoutInflater layoutInflater = LayoutInflater.from(view.getContext());
                View popupView = layoutInflater.inflate(R.layout.dialog_app, null);
                TextView appLabel = popupView.findViewById(R.id.appLabel);
                appLabel.setText(packageData.label);
                TextView appPackageName = popupView.findViewById(R.id.appPackageName);
                appPackageName.setText(packageData.packageName);
                TextView appUserIdDialog = popupView.findViewById(R.id.appUserIdDialog);
                appUserIdDialog.setText(String.valueOf(packageData.userId));

                final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                popupWindow.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                popupWindow.setElevation(10);
                popupWindow.setFocusable(true);
                popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

                Button button = popupView.findViewById(R.id.button);
                button.setText(R.string.delete);
                button.setOnClickListener(v -> {
                    new AlertDialog.Builder(context)
                            .setTitle(R.string.confirm)
                            .setMessage(R.string.confirm_delete)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    AppDatabase db = Util.getDatabase(context);
                                    db.packageDAO().delete(packageData.packageName, packageData.userId);
                                    db.close();
                                    popupWindow.dismiss();
                                    reloadAppList();
                                }
                            })
                            .setNegativeButton(R.string.no, null)
                            .show();
                });
            }
        });

        reloadAppList();

        if (!isNotificationServiceRunning()) {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        }

        String channelName = getString(R.string.channel_id);
        NotificationChannel channel = new NotificationChannel(channelName, channelName, NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(getString(R.string.channel_description));
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    protected void onResume() {
        super.onResume();
        findViewById(R.id.progressAnimation).setVisibility(View.GONE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                String appPackageName = data.getStringExtra("packageName");
                int appUserId = data.getIntExtra("userId", -1);
                String label = data.getStringExtra("label");
                AppDatabase db = Util.getDatabase(context);
                db.packageDAO().insert(new PackageData(appPackageName, appUserId, label));
                db.close();
                reloadAppList();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, R.string.permission_warning, Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void reloadAppList() {
        AppDatabase db = Util.getDatabase(context);
        List<PackageData> appList = db.packageDAO().getAll();
        db.close();
        currentAppListAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, appList);
        currentAppListView.setAdapter(currentAppListAdapter);
    }

    private boolean isNotificationServiceRunning() {
        ContentResolver contentResolver = getContentResolver();
        String enabledNotificationListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners");
        String packageName = getPackageName();
        return enabledNotificationListeners != null && enabledNotificationListeners.contains(packageName);
    }
}