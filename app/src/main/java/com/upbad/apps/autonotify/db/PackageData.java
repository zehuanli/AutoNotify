package com.upbad.apps.autonotify.db;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(primaryKeys = {"packageName", "userId"})
public class PackageData {

    public static final String TABLE_NAME = "PackageData";

    @ColumnInfo(name = "packageName")
    @NonNull
    public String packageName;

    @ColumnInfo(name = "userId")
    @NonNull
    public int userId;

    @ColumnInfo(name = "label")
    public String label;

    @Ignore
    public Drawable icon;

    @Ignore
    public ApplicationInfo applicationInfo;

    public PackageData() {
    }

    @Ignore
    public PackageData(String packageName, int userId, String label) {
        this.packageName = packageName;
        this.userId = userId;
        this.label = label;
    }

    @Override
    public String toString() {
        return (label != null ? label : packageName) + " (" + userId + ")";
    }
}
