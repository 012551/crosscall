package com.adups.fota.manager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.text.TextUtils;

import java.util.List;
import com.adups.fota.GoogleOtaClient;
import com.adups.fota.utils.LogUtil;

import java.util.Stack;

public class ActivityManager {

    private static ActivityManager instance;
    private static Stack<Activity> stack;

    private static boolean isMainActivityTop;

    private ActivityManager() {
        stack = new Stack<>();
    }

    public synchronized static ActivityManager getManager() {
        if (instance == null) {
            instance = new ActivityManager();
        }
        return instance;
    }

    public void pushActivity(Activity activity) {
        if (!stack.contains(activity))
            stack.push(activity);
    }

    public void removeActivity(Activity activity) {
        if (activity != null && stack.contains(activity)) {
            stack.remove(activity);
        }
    }

    public void finishMainActivity() {
        for (Activity activity : stack) {
            if (activity.getClass().equals(GoogleOtaClient.class)) {
                activity.finish();
                return;
            }
        }
    }

    public void finishAllActivity() {
        for (Activity activity : stack) {
                activity.finish();
            }
        stack.clear();
        }


    public boolean isActivityTop(Class clazz) {
        if (!stack.isEmpty()) {
            Activity activity = stack.peek();
            return activity.getClass().equals(clazz);
        }
        return false;
    }

    public boolean isMainActivityTop() {
        return isMainActivityTop;
    }

    public void setTopActivity(boolean value) {
        isMainActivityTop = value;
    }

    /**
     * 判断某个界面是否在前台,返回true，为显示,否则不是
     */
    public static boolean isForeground(Context context, String className) {
        LogUtil.d("");
        if (context == null || TextUtils.isEmpty(className))
            return false;
        android.app.ActivityManager am = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<android.app.ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            if (className.equals(cpn.getClassName()))
                return true;
        }
        return false;
    }

}
