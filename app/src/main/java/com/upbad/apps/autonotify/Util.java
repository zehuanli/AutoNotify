package com.upbad.apps.autonotify;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.room.Room;

import com.upbad.apps.autonotify.db.AppDatabase;
import com.upbad.apps.autonotify.db.PackageData;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    // Reference: https://github.com/ukanth/afwall/
    //   - https://github.com/ukanth/afwall/blob/12c012c4e6bc832a212d29a5783fd93f817398b9/app/src/main/java/dev/ukanth/ufirewall/Api.java#L1526
    //   - https://github.com/ukanth/afwall/blob/12c012c4e6bc832a212d29a5783fd93f817398b9/app/src/main/java/dev/ukanth/ufirewall/Api.java#L1762
    public static List<PackageData> getPackagesForAllUsers(Context context) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        PackageManager packageManager = context.getPackageManager();

        List<Integer> userIdList = new ArrayList<>();
        List<UserHandle> users = userManager.getUserProfiles();
        for (UserHandle user : users) {
            int userId = getUserIdFromUserHandle(user);
            if (userId >= 0) {
                userIdList.add(userId);
            }
        }

        List<ApplicationInfo> applications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA | PackageManager.GET_UNINSTALLED_PACKAGES);

        List<PackageData> packageDataList = new ArrayList<>();
        for (ApplicationInfo applicationInfo : applications) {
            for (int userId : userIdList) {
                try {
                    int packageUid = Integer.parseInt(userId + "" + applicationInfo.uid);
                    String[] packages = packageManager.getPackagesForUid(packageUid);
                    if (packages != null && packages.length > 0) {
                        PackageData packageData = new PackageData();
                        packageData.packageName = applicationInfo.packageName;
                        packageData.userId = userId;
                        packageData.label = packageManager.getApplicationLabel(applicationInfo).toString();
                        packageData.icon = packageManager.getApplicationIcon(applicationInfo);
                        packageData.applicationInfo = applicationInfo;
                        packageDataList.add(packageData);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return packageDataList;
    }

    private static final Pattern USER_HANDLE_PATTERN = Pattern.compile("UserHandle\\{(.*)\\}");
    public static int getUserIdFromUserHandle(UserHandle userHandle) {
        Matcher matcher = USER_HANDLE_PATTERN.matcher(userHandle.toString());
        if (matcher.find()) {
            int userId = Integer.parseInt(matcher.group(1));
            if (userId >= 0) {
                return userId;
            }
        }
        return -1;
    }

    public static AppDatabase getDatabase(Context context) {
        return Room.databaseBuilder(context, AppDatabase.class, Config.DATABASE_FILENAME).allowMainThreadQueries().build();
    }

    public static String getLabelFromPackageName(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA | PackageManager.GET_UNINSTALLED_PACKAGES);
            return packageManager.getApplicationLabel(applicationInfo).toString();
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    public static Drawable getIconFromPackageName(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            return packageManager.getApplicationIcon(packageName);
        } catch (Exception ignored) {
        }
        return null;
    }

    // Reference: https://www.programmersought.com/article/4179688739/
    public static Bitmap drawableToBitmap(Drawable drawable) {
        try {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            } else if (drawable instanceof AdaptiveIconDrawable) {
                Drawable backgroundDr = ((AdaptiveIconDrawable) drawable).getBackground();
                Drawable foregroundDr = ((AdaptiveIconDrawable) drawable).getForeground();

                Drawable[] drr = new Drawable[2];
                drr[0] = backgroundDr;
                drr[1] = foregroundDr;

                LayerDrawable layerDrawable = new LayerDrawable(drr);

                int width = layerDrawable.getIntrinsicWidth();
                int height = layerDrawable.getIntrinsicHeight();

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

                Canvas canvas = new Canvas(bitmap);

                layerDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                layerDrawable.draw(canvas);

                return bitmap;
            }
        } catch (Exception ignored) {
        }

        return null;
    }
}
