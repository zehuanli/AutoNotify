package com.upbad.apps.autonotify.component;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

    private Context context;
    private TextView appTextView;
    private Button addAppButton;
    SwipeRefreshLayout swipeRefreshLayout;
    private ListView currentAppListView;

    ArrayAdapter<PackageData> currentAppListAdapter;

    private String appPackageName;
    private String appLabel;
    private int appUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;
        appTextView = findViewById(R.id.appTextView);
        addAppButton = findViewById(R.id.addAppButton);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        currentAppListView = findViewById(R.id.currentAppListView);

        appTextView.setOnClickListener(v -> {
            Intent startIntent = new Intent(context, AppSearchActivity.class);
            startActivityForResult(startIntent, 1);
        });

        addAppButton.setOnClickListener(v -> {
            if (appPackageName != null) {
                AppDatabase db = Util.getDatabase(context);
                db.packageDAO().insert(new PackageData(appPackageName, appUserId));
                db.close();
                reloadAppList();
            } else {
                Toast.makeText(context, R.string.invalid_app, Toast.LENGTH_SHORT).show();
            }
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
                appLabel.setVisibility(View.GONE);
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                appPackageName = data.getStringExtra("packageName");
                appLabel = data.getStringExtra("label");
                appUserId = data.getIntExtra("userId", -1);
                appTextView.setText(getResources().getString(R.string.label_userId, appLabel, appUserId));
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
}