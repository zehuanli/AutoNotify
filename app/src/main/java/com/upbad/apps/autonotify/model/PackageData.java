package com.upbad.apps.autonotify.model;

import android.content.pm.ApplicationInfo;
import android.graphics.drawable.Drawable;

public class PackageData {
    public String packageName;
    public int userId;
    public String label;
    public Drawable icon;
    public ApplicationInfo applicationInfo;

    @Override
    public String toString() {
        return label;
    }
}
