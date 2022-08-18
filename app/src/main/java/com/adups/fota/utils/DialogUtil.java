package com.adups.fota.utils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.hardware.location.ContextHubManager;
import android.os.Trace;
import android.text.Html;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;

import com.adups.fota.GoogleOtaClient;
import com.adups.fota.config.Setting;
import com.adups.fota.config.TaskID;
import com.adups.fota.view.CheckTextView;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.adups.fota.MaterialDialog;
import com.adups.fota.MyApplication;
import com.adups.fota.R;
import com.adups.fota.bean.LanguageBean;
import com.adups.fota.bean.VersionBean;
import com.adups.fota.callback.ClickCallback;
import com.adups.fota.query.QueryInfo;
import com.adups.fota.view.ProgressLayout;

import java.util.ArrayList;

import static android.security.KeyStore.getApplicationContext;

public class DialogUtil {

    private static MaterialDialog dialog,slient_dialog;
    private static TextView mUpdateTip, mReleaseView;
    private static TextView mReleaseNote;
    private static Button mNight_update;
    private static String mLanguageStr;
    private static ProgressLayout mProgress;
    public static MaterialDialog showCustomDialog(Context context, int resId, DialogInterface.OnDismissListener dismissListener) {
        View view = LayoutInflater.from(context).inflate(resId, null);
        return showSystemDialog(context, null, null, null, null, null,
                null, dismissListener, view, false);
    }

    public static MaterialDialog showPositiveDialog(Context context, String title, String content) {
        return showDialog(context, title, content,
                context.getString(android.R.string.ok), null,
                null, null, null);
    }

    public static MaterialDialog showPositiveDialog(Context context, String title, String content,
                                                    DialogInterface.OnDismissListener dismissListener) {
        return showDialog(context, title, content,
                context.getString(android.R.string.ok), null,
                null, null,
                dismissListener);
    }

    public static MaterialDialog showPositiveDialog(Context context, String title, String content,
                                                    ClickCallback positiveButtonListener,
                                                    DialogInterface.OnDismissListener dismissListener,int gravity) {
        return showSystemDialog(context, title, content,
                "Confirm", positiveButtonListener,
                null, null,
                dismissListener,
                gravity, Gravity.NO_GRAVITY,
                null, false);
    }

    public static MaterialDialog showPositiveDialog(Context context, String title, String content,
                                                    ClickCallback positiveButtonListener,
                                                    DialogInterface.OnDismissListener dismissListener) {
        return showDialog(context, title, content,
                context.getString(android.R.string.ok), positiveButtonListener,
                null, null,
                dismissListener);
    }

    public static MaterialDialog showDialog(Context context, String title, String content,
                                            ClickCallback positiveButtonListener) {
        return showDialog(context, title, content,
                context.getString(android.R.string.ok), positiveButtonListener,
                context.getString(android.R.string.cancel), null);
    }

    public static MaterialDialog showDialog(Context context, String title, String content,
                                            ClickCallback positiveButtonListener, DialogInterface.OnDismissListener dismissListener) {
        return showSystemDialog(context, title, content,
                context.getString(android.R.string.ok), positiveButtonListener,
                context.getString(android.R.string.cancel), null, dismissListener,
                null, false);
    }

    public static MaterialDialog showDialog(Context context, String title, String content,
                                            ClickCallback positiveButtonListener,
                                            ClickCallback negativeButtonListener) {
        return showDialog(context, title, content,
                context.getString(android.R.string.ok), positiveButtonListener,
                context.getString(android.R.string.cancel), negativeButtonListener);
    }

    public static MaterialDialog showBaseCustomDialog(Context context, int imageId, String text) {
        return showBaseCustomDialog(context, imageId, text, null);
    }

    public static MaterialDialog showBaseCustomDialog(Context context, int imageId, String text,
                                                      DialogInterface.OnDismissListener dismissListener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_prompt_base, null);
        ImageView imageView = view.findViewById(R.id.dialog_prompt_icon);
        TextView textView = view.findViewById(R.id.dialog_prompt_content);
        imageView.setBackgroundResource(imageId);
        textView.setText(text);
        return showCustomDialog(context, context.getString(android.R.string.ok), dismissListener, view, false);
    }

    public static MaterialDialog showBaseCustomDialog(Context context, int imageId, String text, int gravity,
                                                      String positiveButtonText, ClickCallback positiveButtonListener,
                                                      DialogInterface.OnDismissListener dismissListener) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_prompt_base, null);
        ImageView imageView = view.findViewById(R.id.dialog_prompt_icon);
        TextView textView = view.findViewById(R.id.dialog_prompt_content);
        imageView.setBackgroundResource(imageId);
        textView.setText(text);
        return showSystemDialog(context, null, null,
                positiveButtonText, positiveButtonListener,
                null, null,
                dismissListener,
                Gravity.START, gravity,
                view, false);
    }

    public static MaterialDialog showNoButtonCustomDialog(Context context, String title,
                                                          DialogInterface.OnDismissListener dismissListener, View view) {
        return showSystemDialog(context, title, null,
                null, null,
                null, null,
                dismissListener,
                Gravity.CENTER, Gravity.END,
                view, false);
    }

    public static MaterialDialog showNoButtonCustomDialog(Context context, String title, int gravity,
                                                          DialogInterface.OnDismissListener dismissListener, View view) {
        return showSystemDialog(context, title, null,
                null, null,
                null, null,
                dismissListener,
                gravity, Gravity.END,
                view, false);
    }

    public static MaterialDialog showCustomDialog(Context context, String title, View view) {
        return showSystemDialog(context, title, null,
                null, null,
                null, null,
                null,
                view, false);
    }

    public static MaterialDialog showCustomDialog(Context context, String title,
                                                  DialogInterface.OnDismissListener dismissListener, View view) {
        return showSystemDialog(context, title, null,
                context.getString(android.R.string.ok), null,
                null, null,
                dismissListener,
                view, false);
    }

    public static MaterialDialog showCustomDialog(Context context, String title,
                                                  String positiveButtonText, final ClickCallback positiveButtonListener,
                                                  String negativeButtonText, final ClickCallback negativeButtonListener,
                                                  DialogInterface.OnDismissListener dismissListener,
                                                  View view) {
        return showSystemDialog(context, title, null,
                positiveButtonText, positiveButtonListener,
                negativeButtonText, negativeButtonListener,
                dismissListener,
                view, false);
    }

    public static MaterialDialog showCustomDialog(Context context,
                                                  String positiveButtonText,
                                                  DialogInterface.OnDismissListener dismissListener,
                                                  View view, boolean cancelable) {
        return showSystemDialog(context, null, null,
                positiveButtonText, null,
                null, null,
                dismissListener,
                view, false);
    }

    public static MaterialDialog showDialog(Context context, String title, String content,
                                            String positiveButtonText, final ClickCallback positiveButtonListener,
                                            String negativeButtonText, final ClickCallback negativeButtonListener) {
        return showSystemDialog(context, title, content,
                positiveButtonText, positiveButtonListener,
                negativeButtonText, negativeButtonListener,
                null,
                null, false);
    }

    public static MaterialDialog showDialog(Context context, String title, String content,
                                            String positiveButtonText, final ClickCallback positiveButtonListener,
                                            String negativeButtonText, final ClickCallback negativeButtonListener,
                                            DialogInterface.OnDismissListener dismissListener) {
        return showSystemDialog(context, title, content,
                positiveButtonText, positiveButtonListener,
                negativeButtonText, negativeButtonListener,
                dismissListener,
                null, false);
    }

    //zhangzhou
    public static MaterialDialog showSlientDialog(Context context, String title, String content) {
        slient_dialog = new MaterialDialog(context, R.style.Dialog);
        slient_dialog.setTitle(title);
        slient_dialog.setCancelable(true);
//        dialog.setTitleGravity(titleGravity);
        slient_dialog.setMessage(content);
        slient_dialog.setCheckSlient();
        if (!TextUtils.isEmpty(content)) LogUtil.d(content);
//        dialog.setBottomGravity(bottomGravity);
        WindowManager windowManager = (WindowManager)
                getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display d = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams p = slient_dialog.getWindow().getAttributes();
        slient_dialog.getWindow().setAttributes(p);
        slient_dialog.show();
        return slient_dialog;
    }

    public static MaterialDialog showDialog(final Context context, String title, final String content,
                                            String positiveButtonText, final ClickCallback positiveButtonListener,
                                            String negativeButtonText, final ClickCallback negativeButtonListener,
                                            String neutralButtonText, final ClickCallback neutralButtonListener,
                                            View customView,
                                            DialogInterface.OnDismissListener dismissListener) {

        dialog = new MaterialDialog(context, R.style.Dialog);
        dialog.setTitle(title);
//        dialog.setTitleGravity(titleGravity);
        dialog.setMessage(content);
        if (!TextUtils.isEmpty(content)) LogUtil.d(content);
//        dialog.setBottomGravity(bottomGravity);
        dialog.setPositiveButton(positiveButtonText, positiveButtonListener);
        dialog.setNegativeButton(negativeButtonText, negativeButtonListener);
        dialog.setNeutralButton(neutralButtonText,neutralButtonListener);
        dialog.setOnDismissListener(dismissListener);

        mProgress = customView.findViewById(R.id.dialog_progress_layout);
        mNight_update = customView.findViewById(R.id.night_update_button);//zhangshou
        mReleaseNote = customView.findViewById(R.id.relese_note);
        mReleaseView = customView.findViewById(R.id.relese_view);
        mUpdateTip = customView.findViewById(R.id.dialog_ota_update_tip);

        initNewVersionView();
        WindowManager windowManager = (WindowManager)
                getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display d = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
        p.width = (int) (d.getWidth() * 0.90);
        p.height = (int) (d.getHeight() * 0.90);
        dialog.getWindow().setAttributes(p);
        if (customView != null) {
            dialog.setContentView(customView);
        }

        mNight_update.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                LogUtil.d("enter Night_update");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(getApplicationContext(), GoogleOtaClient.class);
                        intent.putExtra(Setting.INTENT_PARAM_FLAG, Setting.INTENT_PARAM_NIGHT);
                        context.startActivity(intent);
                    }
                }).start();
            }
        });

        dialog.show();
        return dialog;
//        return showSystemDialog(context, title, content,
//                positiveButtonText, positiveButtonListener,
//                negativeButtonText, negativeButtonListener,
//                neutralButtonText,neutralButtonListener,
//                dismissListener,
//                null, false);
    }

    public static MaterialDialog showSystemDialog(Context context, String title, String content,
                                                  String positiveButtonText, final ClickCallback positiveButtonListener,
                                                  String negativeButtonText, final ClickCallback negativeButtonListener,
                                                  DialogInterface.OnDismissListener dismissListener,
                                                  View customView, boolean isSystem) {
        return showSystemDialog(context, title, content,
                positiveButtonText, positiveButtonListener,
                negativeButtonText, negativeButtonListener,
                dismissListener,
                Gravity.START, Gravity.END,
                customView, isSystem);
    }

    private static MaterialDialog showSystemDialog(Context context, String title, String content,
                                                   String positiveButtonText, final ClickCallback positiveButtonListener,
                                                   String negativeButtonText, final ClickCallback negativeButtonListener,
                                                   DialogInterface.OnDismissListener dismissListener,
                                                   int titleGravity, int bottomGravity,
                                                   View customView, boolean isSystem) {
        dialog = new MaterialDialog(context, R.style.Dialog);
        dialog.setTitle(title);
        dialog.setTitleGravity(titleGravity);
        dialog.setMessage(content);
        if (!TextUtils.isEmpty(content)) LogUtil.d(content);
        dialog.setBottomGravity(bottomGravity);
        dialog.setPositiveButton(positiveButtonText, positiveButtonListener);
        dialog.setNegativeButton(negativeButtonText, negativeButtonListener);
        dialog.setOnDismissListener(dismissListener);
        if (customView != null)
            dialog.setContentView(customView);
        dialog.show();
        return dialog;
    }

    public static void closeDialog() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
            LogUtil.d("closeDialog");
        }
//        if (dialog != null && dialog.isShowing()) {
//            try {
//                Context context = ((ContextWrapper)dialog.getContext()).getBaseContext();
//                if(context instanceof Activity) {
//                    if(!((Activity)context).isFinishing() && !((Activity)context).isDestroyed())
//                        dialog.dismiss();
//                } else //if the Context used wasnt an Activity, then dismiss it too
//                    dialog.dismiss();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }finally {
//                dialog = null;
//            }
//            LogUtil.d("closeDialog");
//        }
//        dialog = null;
    }

    private static void loadReleaseNotes() {
        LogUtil.d("enter");

        //zhangzhou start
        String version_data = null;
        String textArrar[]=null;
        //zhangzhou end

        VersionBean version = QueryInfo.getInstance().getVersionInfo();
        if (version != null) {

            //zhangzhou
            version_data = QueryInfo.getInstance().getReleaseNotes(MyApplication.getAppContext(),true);
            LogUtil.d("version_data:" + version_data);
            textArrar=version_data.split("</div>");
            String version_info =textArrar[0] + "</div>";
            String version_note =textArrar[1] + "</div>";
            version_note = version_note.replaceFirst("<br/>", "");
            version_info = Html.fromHtml(version_info).toString().trim();
            LogUtil.d("版本信息："+version_info);
            LogUtil.d("修改点：："+version_note);
            mReleaseView.setText(version_info);
            mReleaseNote.setText(Html.fromHtml(version_note));
            //zhangzhou

            LogUtil.d("loadReleaseNotes:"+QueryInfo.getInstance().getNowLanguage());
            if (QueryInfo.getInstance().getNowLanguage().equals("zh_CN")){
                Typeface typeFace = Typeface.createFromAsset(MyApplication.getAppContext().getAssets(), "fonts/wen_quan.ttf");
                mReleaseView.setTypeface(typeFace);
            } else {
                mReleaseView.setTypeface(null);
            }
        }
        LogUtil.d("exit");
    }


    private static void initNewVersionView() {
        LogUtil.d("enter");
        loadReleaseNotes();
        mUpdateTip.setVisibility(View.VISIBLE);
        mProgress.reset();
        mProgress.setVersionTip(MyApplication.getAppContext().getString(R.string.new_version_text));
        LogUtil.d("exit");
    }

}
