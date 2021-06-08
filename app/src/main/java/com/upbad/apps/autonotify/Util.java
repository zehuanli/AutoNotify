package com.upbad.apps.autonotify;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

import com.upbad.apps.autonotify.model.PackageData;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

    private static final Pattern USER_HANDLE_PATTERN = Pattern.compile("UserHandle\\{(.*)\\}");

    // Reference: https://github.com/ukanth/afwall/
    //   - https://github.com/ukanth/afwall/blob/12c012c4e6bc832a212d29a5783fd93f817398b9/app/src/main/java/dev/ukanth/ufirewall/Api.java#L1526
    //   - https://github.com/ukanth/afwall/blob/12c012c4e6bc832a212d29a5783fd93f817398b9/app/src/main/java/dev/ukanth/ufirewall/Api.java#L1762
    public static List<PackageData> getPackagesForAllUsers(Context context) {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        PackageManager packageManager = context.getPackageManager();

        List<Integer> userIdList = new ArrayList<>();
        List<UserHandle> users = userManager.getUserProfiles();
        for (UserHandle user : users) {
            Matcher matcher = USER_HANDLE_PATTERN.matcher(user.toString());
            if (matcher.find()) {
                int userId = Integer.parseInt(matcher.group(1));
                if (userId >= 0) {
                    userIdList.add(userId);
                }
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
}
