package com.adups.fota.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import com.adups.fota.MyApplication;
import com.adups.fota.R;
import com.adups.fota.activity.FileBrowserActivity;
import com.adups.fota.activity.SettingActivity;
import com.adups.fota.bean.EventMessage;
import com.adups.fota.config.Event;
import com.adups.fota.config.Setting;
import com.adups.fota.config.Status;
import com.adups.fota.utils.DeviceInfoUtil;
import com.adups.fota.utils.PreferencesUtils;
import com.adups.fota.utils.ToastUtil;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;

/**
 * Created by brave on 2016/1/10.
 */
public class PopWindowsLayout {

    private PopupWindow set_menu_win;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public void setPopWinLayout(final Activity activity, View clickView) {
        View popupWindow_view = LayoutInflater.from(activity).inflate(R.layout.settings_pop, null);

        set_menu_win = new PopupWindow(popupWindow_view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        set_menu_win.setFocusable(true);
        set_menu_win.setBackgroundDrawable(new ColorDrawable(0x00000000));

        int mPopRightWidth = phoneWidthPixels(activity) / 60;
        int titleHeight = (int) activity.getResources().getDimension(R.dimen.activity_title_height);
        int statusBarHeight = getStatusBarHeight();
        int offset = (int) dpToPx(activity.getBaseContext(), 8.0f);
        int gravity = Gravity.END | Gravity.TOP;
        if (View.LAYOUT_DIRECTION_RTL == TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()))
            gravity = Gravity.START | Gravity.TOP;
        set_menu_win.showAtLocation(clickView, gravity, mPopRightWidth, titleHeight + statusBarHeight - offset);


        LinearLayout pop_file_select = popupWindow_view.findViewById(R.id.pop_file_select);
        LinearLayout pop_full_check = popupWindow_view.findViewById(R.id.pop_full_check);
        LinearLayout pop_setting = popupWindow_view.findViewById(R.id.pop_setting);
        LinearLayout pop_exit = popupWindow_view.findViewById(R.id.pop_exit);
        if (DeviceInfoUtil.getInstance().isNoTouch()) {
            pop_file_select.requestFocus();
            pop_file_select.setFocusable(true);
            pop_full_check.setFocusable(true);
            pop_setting.setFocusable(true);
            pop_exit.setFocusable(true);
        }

        if (!DeviceInfoUtil.getInstance().isShowLocalUpdate()) {
            pop_file_select.setVisibility(View.GONE);
        }

        if (!DeviceInfoUtil.getInstance().isShowExit()) {
            pop_exit.setVisibility(View.GONE);
        }

        //后台配置为打开才显示，默认关闭
        if (PreferencesUtils.getInt(activity, Setting.QUERY_FULL, 0) == 1) {
            pop_full_check.setVisibility(View.VISIBLE);
        }

        //文件选择
        pop_file_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set_menu_win.dismiss();
                set_menu_win = null;
                int version_status = Status.getVersionStatus(MyApplication.getAppContext());

                if (version_status == Status.STATE_AB_UPDATING || version_status == Status.STATE_DOWNLOADING) {
                    ToastUtil.showToast(R.string.tips_abDownOrInstall);
                } else {
                    activity.startActivity(new Intent(activity.getBaseContext(), FileBrowserActivity.class));
                }
            }
        });

        // 全量包检测
        pop_full_check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set_menu_win.dismiss();
                set_menu_win = null;
                int version_status = Status.getVersionStatus(MyApplication.getAppContext());
                if (version_status == Status.STATE_AB_UPDATING) {
                    ToastUtil.showToast(R.string.tips_abInstall);
                } else {
                    EventBus.getDefault().post(new EventMessage(Event.QUERY, Event.QUERY_FULL_ROM, 0, 0, null));
                }

            }
        });

        //设置
        pop_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                set_menu_win.dismiss();
                set_menu_win = null;
                activity.startActivity(new Intent(activity.getBaseContext(), SettingActivity.class));
            }
        });
        //退出应用
        pop_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.finish();
            }
        });
    }

    private float dpToPx(Context context, float dp) {
        if (context == null) {
            return -1;
        }
        return dp * context.getResources().getDisplayMetrics().density;
    }

    //获取屏幕宽度
    private int phoneWidthPixels(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    //获取通知栏分辨率
    private int getStatusBarHeight() {
        return Resources.getSystem().getDimensionPixelSize(
                Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android"));
    }

}
