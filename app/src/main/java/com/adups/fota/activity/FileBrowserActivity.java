package com.adups.fota.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.adups.fota.R;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.StorageUtil;
import com.adups.fota.utils.ToastUtil;
import com.adups.fota.view.TitleView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;

public class FileBrowserActivity extends BaseActivity {

    private static final String SDCARD_ROOT_DEFAULT = "/storage";
    private static final int REQUEST_CODE = 1;
    private static int mPosition;
    private static Stack<Integer> mPositionStack = new Stack<>();
    private boolean isRoot = false;
    private ListView mListView;
    private List<FileItem> mData;
    private String mDir;
    private String outSdcard;
    private String innerSdcard;
    private TextView dirTextView;

    @Override
    protected void setTitleView(TitleView titleView) {
        titleView.setContent(getString(R.string.option_file_select));
    }

    @Override
    public void initWidget() {
        setContentView(R.layout.activity_file_browser);
        initViews();
        checkPermission();
    }

    @Override
    public void widgetClick(View v) {

    }

    private void initViews() {
        outSdcard = StorageUtil.getStoragePath(this, true);
        innerSdcard = StorageUtil.getStoragePath(this, false);
        mData = new ArrayList<>();
        FileItem fileItem;
        if (!TextUtils.isEmpty(outSdcard)) {
            fileItem = new FileItem(getString(R.string.out_sdcard), outSdcard, false);
            mData.add(fileItem);
        }
        if (!TextUtils.isEmpty(innerSdcard)) {
            fileItem = new FileItem(getString(R.string.inner_sdcard), innerSdcard, false);
            mData.add(fileItem);
        }
        mListView = findViewById(R.id.file_list_view);
        dirTextView = findViewById(R.id.option_file_select_dir);
        dirTextView.setText(R.string.selected_update_zip);
        initData();
    }

    private void initData() {
        FileAdapter adapter = new FileAdapter(this);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                    long arg3) {
                mPositionStack.push(mPosition);
                isRoot = false;
                LogUtil.d("initListViews, " + mData.get(arg2).filePath);
                if (!mData.get(arg2).isZip) {
                    File f = new File(mData.get(arg2).filePath);
                    File[] files = f.listFiles();
                    int count = 0;
                    if (files != null) {
                        for (File file : files) {
                            if (file.isDirectory()) {
                                count++;
                                break;
                            } else {
                                if (file.getName().toLowerCase(Locale.US).endsWith(".zip")) {
                                    count++;
                                    break;
                                }
                            }
                        }
                    }
                    if (count > 0) {
                        mDir = mData.get(arg2).filePath;
                        mData = getData();
                        FileAdapter adapter = new FileAdapter(FileBrowserActivity.this);
                        mListView.setAdapter(adapter);
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                                !(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
                            requestPermissions(
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
                        } else
                            ToastUtil.showToast(R.string.empty_directory);
                    }
                } else {
                    finishWithResult(mData.get(arg2).filePath);
                }
            }

        });
        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {

            }

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE) {
                    mPosition = mListView.getFirstVisiblePosition();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (isRoot || TextUtils.isEmpty(mDir)) {
            mPositionStack.clear();
            super.onBackPressed();
        } else if (!TextUtils.isEmpty(innerSdcard) && mDir.equals(innerSdcard)) {
            isRoot = true;
            mDir = new File(innerSdcard).getParent();
            initViews();
        } else if (!TextUtils.isEmpty(outSdcard) && mDir.equals(outSdcard)) {
            isRoot = true;
            mDir = new File(outSdcard).getParent();
            initViews();
        } else {
            isRoot = false;
            File f = new File(mDir);
            mDir = f.getParent();
            mData = getData();
            FileAdapter adapter = new FileAdapter(FileBrowserActivity.this);
            mListView.setAdapter(adapter);
            if (mPositionStack.size() > 0) {
                int first = mPositionStack.pop();
                mListView.setSelection(first);
            }
        }
    }

    private List<FileItem> getData() {
        List<FileItem> list = new ArrayList<>();
        File[] files;
        files = getSdcardList();
        if (!TextUtils.isEmpty(innerSdcard) && mDir.contains(innerSdcard)) {
            dirTextView.setVisibility(View.VISIBLE);
            dirTextView.setText(mDir.replace(innerSdcard, getString(R.string.inner_sdcard)));
        } else if (!TextUtils.isEmpty(outSdcard) && mDir.contains(outSdcard)) {
            dirTextView.setVisibility(View.VISIBLE);
            dirTextView.setText(mDir.replace(outSdcard, getString(R.string.out_sdcard)));
        }
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    list.add(new FileItem(file.getName(), file.getPath(), false));
                } else {
                    if (file.getName().toLowerCase(Locale.US).endsWith(".zip")) {
                        list.add(new FileItem(file.getName(), file.getPath(), true));
                    }
                }
            }
        }
        return list;
    }

    //过滤 SD卡
    private File[] getSdcardList() {
        File[] files = new File(mDir).listFiles();
        ArrayList<String> newFile = new ArrayList<>();
        if (mDir.equals(SDCARD_ROOT_DEFAULT)) {
            if (StorageUtil.isPathSeted()) {
                if (StorageUtil.isSdcardAvailable(getApplicationContext())) {
                    File[] newFiles0 = new File[1];
                    newFiles0[0] = new File(StorageUtil.getSdcardRoot(getApplicationContext(), false));
                    return newFiles0;
                } else {
                    return new File[0];
                }
            }
            List<StorageUtil.StorageInfo> infos = StorageUtil.getStorageList();
            if (infos != null) {
                for (int i = 0; i < infos.size(); i++) {
                    String infoPath = infos.get(i).path;
                    if ((infoPath != null)
                            && (Environment.MEDIA_MOUNTED.equals(StorageUtil.getExternalStorageStateExt(infoPath)))) {
                        newFile.add(infoPath);
                    }
                }
                if (newFile.size() > 0) {
                    File[] newFiles = new File[newFile.size()];
                    for (int i = 0; i < newFiles.length; i++) {
                        newFiles[i] = new File(newFile.get(i));
                    }
                    return newFiles;
                } else {
                    File[] newFiles2 = new File[1];
                    newFiles2[0] = Environment.getExternalStorageDirectory();
                    return newFiles2;
                }
            } else {
                File[] newFiles3 = new File[1];
                newFiles3[0] = Environment.getExternalStorageDirectory();
                return newFiles3;
            }
        }
        return files;
    }

    private void finishWithResult(String path) {
        Intent intent = new Intent();
        intent.putExtra("selected_file", path);
        intent.setClass(this, SdcardUpdateActivity.class);
        startActivityForResult(intent, 1);
        finish();
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE) initViews();
    }

    public class FileItem {
        String fileName;
        String filePath;
        boolean isZip;

        public FileItem(String fileName, String filePath, boolean isZip) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.isZip = isZip;
        }
    }

    public class FileAdapter extends BaseAdapter {
        private LayoutInflater mInflater;

        public FileAdapter(Context context) {
            this.mInflater = LayoutInflater.from(context);
        }

        public int getCount() {
            return mData.size();
        }

        public Object getItem(int arg0) {
            return mData.get(arg0);
        }

        public long getItemId(int arg0) {
            return 0;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder = null;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.browser_file_list_item, null);
                holder.img = convertView.findViewById(R.id.img);
                holder.title = convertView.findViewById(R.id.title);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            FileItem item = (FileItem) getItem(position);
            if (item.isZip) {
                holder.img.setImageResource(R.mipmap.ex_doc);
            } else {
                holder.img.setImageResource(R.mipmap.ex_folder);
            }
            holder.title.setText(item.fileName);
            return convertView;
        }


        public final class ViewHolder {
            public ImageView img;
            public TextView title;
        }

    }

}
