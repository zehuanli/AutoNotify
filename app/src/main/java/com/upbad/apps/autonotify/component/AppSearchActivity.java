package com.upbad.apps.autonotify.component;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.upbad.apps.autonotify.R;
import com.upbad.apps.autonotify.Util;
import com.upbad.apps.autonotify.db.PackageData;

import java.util.List;

public class AppSearchActivity extends Activity {

    private EditText appSearchText;
    private ListView appListView;
    private SwipeRefreshLayout swipeRefreshLayout;

    private AppSearchAdapter appSearchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_search);

        appSearchText = findViewById(R.id.appSearchText);
        appListView = findViewById(R.id.appListView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        appListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final PackageData packageData = appSearchAdapter.getItem(position);
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
                button.setText(R.string.select);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("packageName", packageData.packageName);
                        resultIntent.putExtra("label", packageData.label);
                        resultIntent.putExtra("userId", packageData.userId);
                        setResult(RESULT_OK, resultIntent);
                        popupWindow.dismiss();
                        finish();
                    }
                });
            }
        });

        appSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                appSearchAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            reloadAppList();
            swipeRefreshLayout.setRefreshing(false);
        });

        reloadAppList();
    }

    private void reloadAppList() {
        List<PackageData> packages = Util.getPackagesForAllUsers(this);

        appSearchAdapter = new AppSearchAdapter(this, R.id.appLabel, packages);
        appListView.setAdapter(appSearchAdapter);

        appSearchText.setHint(getResources().getString(R.string.search_placeholder) + " (" + packages.size() + " records)");
    }
}