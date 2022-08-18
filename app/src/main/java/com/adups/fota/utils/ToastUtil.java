package com.adups.fota.utils;

import android.widget.Toast;

import com.adups.fota.MyApplication;

/**
 * Created by Ruansu on 2018/3/12 0012.
 */

public class ToastUtil {

    private static Toast toast;

    private static Toast getToast() {
        if (toast == null) {
            toast = Toast.makeText(MyApplication.getAppContext(), null, Toast.LENGTH_SHORT);
        }
        return toast;
    }

    public static void showToast(String text) {
        Toast toast = getToast();
        toast.setText(text);
        toast.show();
    }

    public static void showToast(int resId) {
        showToast(MyApplication.getAppContext().getString(resId));
    }

}
