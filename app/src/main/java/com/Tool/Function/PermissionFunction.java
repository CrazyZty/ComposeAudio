package com.Tool.Function;


import android.os.Handler;
import android.os.Looper;

/**
 * Created by zhengtongyu on 16/5/29.
 */
public class PermissionFunction {
    public static void ShowCheckPermissionNotice(final String permissionName) {
        if (CommonFunction.IsInMainThread()) {
            CommonFunction.showToast("你尚未开启" + permissionName + "权限,请开启再次尝试", "PermissionFunction");
        } else {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {

                    CommonFunction.showToast("你尚未开启" + permissionName + "权限,请开启再次尝试", "PermissionFunction");
                }
            });
        }
    }
}
