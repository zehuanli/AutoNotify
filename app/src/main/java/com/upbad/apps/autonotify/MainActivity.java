package com.upbad.apps.autonotify;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private TextView appTextView;
    private Button addAppButton;
    private ListView currentAppListView;

    private String appPackageName;
    private String appLabel;
    private int appUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appTextView = findViewById(R.id.appTextView);
        addAppButton = findViewById(R.id.addAppButton);
        currentAppListView = findViewById(R.id.currentAppListView);

        appTextView.setOnClickListener(v -> {
            Intent startIntent = new Intent(this, AppSearchActivity.class);
            startActivityForResult(startIntent, 1);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                appPackageName = data.getStringExtra("packageName");
                appLabel = data.getStringExtra("label");
                appUserId = data.getIntExtra("userId", -1);
                appTextView.setText(appLabel);
            }
        }
    }
}