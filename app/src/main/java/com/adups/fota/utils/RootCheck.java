package com.adups.fota.utils;

import android.text.TextUtils;

import com.adups.fota.MyApplication;
import com.adups.fota.bean.RootErrBean;
import com.adups.fota.report.ReportData;
import com.google.gson.Gson;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by Administrator on 2016/3/17.
 */
public class RootCheck {
    private List<String> addList;
    private List<String> modifyList;
    private List<String> deleteList;
    private Map<String, String> contMap;
    private RootErrBean rootErrBean;

    private static String getFileMD5(String file) {
        FileInputStream fis = null;
        StringBuilder builder = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            fis = new FileInputStream(file);
            byte[] buffer = new byte[1024 * 1024];
            int length = -1;
            if (md == null) {
                return null;
            }
            while ((length = fis.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
            byte[] bytes = md.digest();
            if (bytes == null) {
                return null;
            }
            for (byte aByte : bytes) {
                String md5s = Integer.toHexString(aByte & 0xff);
                if (md5s.length() == 1) {
                    builder.append("0");
                }
                builder.append(md5s);
            }
            return builder.toString();
        } catch (Exception ex) {
            LogUtil.d("getFileMD5, Exception " + ex.toString());
            ex.printStackTrace();
            return null;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * added brave for
     * 解压缩功能. 注：压缩包有文件夹
     * 将ZIP_FILENAME文件解压到ZIP_DIR目录下.
     *
     * @throws Exception
     */
    public String checkDevicesIsRoot(String file_path) throws Exception {
        contMap = new HashMap<String, String>();
        boolean isZip = FileUtil.isZipFile(file_path);
        LogUtil.d("isZip = " + isZip);
        if (isZip) {
            ZipFile zfile = null;
            try {
                zfile = new ZipFile(file_path);
                ZipEntry ze = zfile.getEntry("RC/checkroot");
                if (ze != null) {
                    InputStream input = zfile.getInputStream(ze);
                    parseRootFile(input);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (zfile != null) {
                    zfile.close();
                }
            }
        } else { //是7z
            //还原头文件信息-20170808
            FileUtil.modifyFileContent(file_path, 0, "7z");

            //调用7z相关方法
            SevenZFile sevenZFile = null;
            try {
                sevenZFile = new SevenZFile(new File(file_path));
                while (true) {
                    SevenZArchiveEntry entry = sevenZFile.getNextEntry();
                    if (entry == null) {
                        LogUtil.d("entry == null");
                        ReportData.postInstall(MyApplication.getAppContext(), ReportData.INSTALL_STATUS_CAUSE_GET_ENTRY_FAILED);
                        break;
                    }
                    if (!entry.getName().equals("RC/checkroot")) {
                        continue;
                    } else {
                        //找到 RC/checkroot 文件进行操作
                        byte[] content = new byte[(int) entry.getSize()];
                        while (sevenZFile.read(content, 0, (int) entry.getSize()) > 0) {
                            InputStream input = new ByteArrayInputStream(content);
                            parseRootFile(input);
                        }
                        break;
                    }
                }
                FileUtil.modifyFileContent(file_path, 0, "8p"); //继续修改头文件信息 - 20170808
            } catch (Exception e) {
                LogUtil.d(e.getMessage());
                ReportData.postInstall(MyApplication.getAppContext(), ReportData.INSTALL_STATUS_CAUSE_UNZIP_FAILED);
            } finally {
                if (sevenZFile != null)
                    sevenZFile.close();
            }
        }
        if (contMap.size() == 0) {
            return "";
        }
        //判断是否root
        try {
            rootErrBean = new RootErrBean();
            addList = new ArrayList<String>();
            modifyList = new ArrayList<String>();
            deleteList = new ArrayList<String>();

            boolean isRootbin = getTrashFiles("/system/bin");
            boolean isRootxbin = getTrashFiles("/system/xbin");

            //计算少了的文件
            if (!isRootbin && !isRootxbin && contMap.size() > 0) {
                for (String key : contMap.keySet()) {
                    if (contMap.get(key) != null && contMap.get(key).equals("ignore")) {
                        continue;
                    }
                    if (deleteList.size() > 5) { //最多5个
                        break;
                    }
                    deleteList.add(key);
                    rootErrBean.setDelete(deleteList);
                }
            }
            int count = addList.size() + modifyList.size() + deleteList.size();
            if (count > 0) {
                Gson g = new Gson();
                LogUtil.d("root gson " + g.toJson(rootErrBean));
                return g.toJson(rootErrBean);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        addList = null;
        modifyList = null;
        deleteList = null;
        contMap = null;
        return "";
    }

    private void parseRootFile(InputStream input) throws IOException {
        BufferedReader br = null;
        BufferedInputStream is = new BufferedInputStream(input);
        try {
            br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.indexOf("\t") > -1) {
                    String[] contents = line.split("\t");
                    if (contents.length > 0) {
                        String fileNameKey = "/system/" + contents[0];
                        try {
                            contMap.put(fileNameKey, contents[1]);
                            LogUtil.d("checkDevicesIsRoot " + fileNameKey + " md5Encode " + contents[1]);
                        } catch (Exception e) {
                            contMap.put(fileNameKey, "");
                        }
                    }
                }
            }
        } finally {
            is.close();
            if (br != null) {
                br.close();
            }
        }
    }

    private boolean getTrashFiles(String path) {
        File[] files = new File(path).listFiles();
        if (files != null) {
            for (final File f : files) {
                if (analyzeResult(f))
                    return true;
            }
        }
        return false;
    }

    private boolean analyzeResult(File file) {
        String filemd5 = "";
        String path = file.getAbsolutePath();
        if (!contMap.containsKey(path)) {
            LogUtil.d("path " + path + " is un exist");
            addList.add(path);
            //记录增加文件
            rootErrBean.setAdd(addList);
        } else if (contMap.containsKey(path)) {
            if (contMap.get(path).equals("ignore")) { //读取到忽略的文件就略过
                contMap.remove(path);
            } else if (file.isDirectory()) {
                LogUtil.d("path " + path + " is directory");
                try {
                    Thread.sleep(5);
                    contMap.remove(path);
                    getTrashFiles(path);
                } catch (Exception e) {

                }
            } else if (file.isFile()) {
                try {
                    filemd5 = getFileMD5(path);
                    // LogUtil.d(LOG_TAG, "path filemd5" + path + filemd5);

                    //读取不到md5及md5一致都将清除内存记录
                    if (TextUtils.isEmpty(filemd5) || contMap.get(path).equals(filemd5)
                    ) {
                        contMap.remove(path);
                    } else { //文件已经被修改
                        modifyList.add(path);
                        //记录增加文件
                        rootErrBean.setModify(modifyList);
                    }
                } catch (Exception e) {
                    contMap.remove(path);
                }
            } else {
                contMap.remove(path);
            }
        }
        int count = addList.size() + modifyList.size();
        return count > 5;

    }


}
