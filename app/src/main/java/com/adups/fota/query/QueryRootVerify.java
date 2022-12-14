package com.adups.fota.query;

import android.os.Build;
import android.text.TextUtils;

import com.adups.fota.bean.RootErrBean;
import com.adups.fota.utils.JsonUtil;
import com.adups.fota.utils.LogUtil;
import com.adups.fota.utils.RootCheck;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipInputStream;

/**
 * whether Rom is damaged before download
 * Created by xw on 15-12-18.
 */
public class QueryRootVerify {

    private static final String LOG_TAG = "QueryRootVerify";

    public static String isRomDamaged(String filePath) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O_MR1) return null;
        try {
            String rootCheckResult = new RootCheck().checkDevicesIsRoot(filePath);
            if (TextUtils.isEmpty(rootCheckResult)) {
                rootCheckResult = QueryRootVerify.checkUpdateFileResult(filePath);
            }
            return rootCheckResult;
        } catch (Exception e) {
            LogUtil.d(e.getMessage());
        }
        return null;
    }

    private static String checkUpdateFileResult(String file_path) {
        String result = null;
        try {
            result = checkSourceFileError(file_path);
        } catch (FileNotFoundException e) {
            LogUtil.e("checkUpdateFileResult, FileNotFoundException = " + e.toString());
        } catch (IOException e) {
            LogUtil.e("checkUpdateFileResult, IOException = " + e.toString());
        }
        return result;
    }

    /**
     * @param file_path update  zip  path
     * @return file name   while  verification  is  error
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static String checkSourceFileError(String file_path) throws IOException {
        RootErrBean json = new RootErrBean();
        List<String> lists = new ArrayList<>();
        BufferedInputStream bi;
        //----????????????(ZIP????????????????????????????????????????????????????????????):
        LogUtil.d("start checkSourceFile");
        FileInputStream fi = new FileInputStream(file_path);
        CheckedInputStream csumi = new CheckedInputStream(fi, new CRC32());
        ZipInputStream in2 = new ZipInputStream(csumi);
        bi = new BufferedInputStream(in2);
        java.util.zip.ZipEntry ze;//??????????????????
        byte[] buffer = new byte[1024];
        //?????????????????????????????????
        while ((ze = in2.getNextEntry()) != null) {
            String entryName = ze.getName();
            // if (entryName.equals("META-INF/com/google/android/updater-script")) {
            if (entryName.equals(String.valueOf(new char[]{'M', 'E', 'T', 'A', '-', 'I', 'N', 'F', '/', 'c', 'o', 'm', '/', 'g', 'o', 'o', 'g', 'l', 'e', '/', 'a', 'n', 'd', 'r', 'o', 'i', 'd', '/', 'u', 'p', 'd', 'a', 't', 'e', 'r', '-', 's', 'c', 'r', 'i', 'p', 't',}))) {
                //?????????????????????
                StringBuilder dataStr = new StringBuilder();
                int readCount = bi.read(buffer);
                while (readCount != -1) {
                    String tt = new String(buffer, 0, readCount);
                    dataStr.append(tt);
                    readCount = bi.read(buffer);
                }
                int pos = 0;
                while (true) {
                    int index = dataStr.indexOf("apply_patch_check(\"/system", pos);
                    if (index >= 0) {
                        pos = index + "apply_patch_check(\"/system".length();
                        index = dataStr.indexOf("\")", pos);
                        if (index < 0) {
                            break;//??????????????????????????????
                        }
                        int tmp_pos = 0;
                        String src_str = dataStr.substring(pos, index);
                        pos = index + "\");".length();
                        index = src_str.indexOf("\", \"", tmp_pos);
                        if (index < 0) {
                            continue;//?????????????????????
                        }
                        String filePath = src_str.substring(tmp_pos, index);
                        tmp_pos = index + "\", \"".length();
                        index = src_str.indexOf("\", \"", tmp_pos);
                        if (index < 0) {
                            continue;//?????????????????????
                        }
                        String sha1_str1 = src_str.substring(tmp_pos, index);
                        tmp_pos = index + "\", \"".length();
                        String sha1_str2 = src_str.substring(tmp_pos);
                        LogUtil.d("file path = " + filePath + "  " + "sha1_str1 = " + sha1_str1 + "  " + "sha1_str2 = " + sha1_str2);
                        try {
                            if (!getFileSha1(filePath, sha1_str1, sha1_str2)) {
                                lists.add(filePath.substring(filePath.lastIndexOf("/") + 1));
                            }
                        } catch (Exception e) {
                            LogUtil.e("checkSourceFileInvaild, Exception = " + e.toString());
                        }
                    } else {
                        break;
                    }
                }
                break;
            }
        }
        bi.close();
        if (lists.size() > 0) {
            json.setModify(lists);
            return JsonUtil.toJson(json);
        } else {
            return null;
        }
    }

    /**
     * ????????????G????????????
     */
    private static boolean getFileSha1(String path, String sha1_str1, String sha1_str2) throws OutOfMemoryError, IOException {
        File file = new File("/system" + path);
        if (!file.exists()) {
            LogUtil.d("/system" + path + " is not exists !");
            return false;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            MessageDigest messagedigest;
            LogUtil.d("getFileSha1()---path = " + "/system" + path + " sha1_str1 = " + sha1_str1 + " sha1_str2 = " + sha1_str2);
            messagedigest = MessageDigest.getInstance("SHA-1");
            byte[] buffer = new byte[1024 * 100];
            int len = 0;
            while ((len = in.read(buffer)) > 0) {
                //????????????????????? update????????????????????????
                messagedigest.update(buffer, 0, len);
            }
            //????????????????????????????????????digest ??????????????????????????????????????? digest ?????????MessageDigest ??????????????????????????????????????????
            String sha_str = byteArrayToHexString(messagedigest.digest());
            sha_str = sha_str.toLowerCase();
            LogUtil.d("getFileSha1()---act_sha1String = " + sha_str);
            return sha_str.equals(sha1_str1) || sha_str.equals(sha1_str2);
        } catch (NoSuchAlgorithmException e) {
            LogUtil.e("getFileSha1, NoSuchAlgorithmException = " + e.toString());
        } catch (OutOfMemoryError e) {
            LogUtil.e("getFileSha1, OutOfMemoryError = " + e.toString());
        }
        return false;
    }

    // ???????????????????????????????????????
    private static String byteToHexString(byte ib) {
        char[] Digit = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C',
                'D', 'E', 'F'
        };
        char[] ob = new char[2];
        ob[0] = Digit[(ib >>> 4) & 0X0F];
        ob[1] = Digit[ib & 0X0F];
        return new String(ob);
    }

    // ?????????????????????????????????????????????
    private static String byteArrayToHexString(byte[] byteArray) {
        StringBuilder strDigest = new StringBuilder();
        for (byte b : byteArray) {
            strDigest.append(byteToHexString(b));
        }
        return strDigest.toString();
    }

}
