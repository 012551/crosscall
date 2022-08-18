package com.adups.fota.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;

import net.lingala.zip4j.model.FileHeader;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class FileUtil {

    public static void write2Sd(String path, String s) {
        if (TextUtils.isEmpty(path)) return;
        File f = new File(path);
        File folder = new File(f.getParent());
        if (!folder.exists())
            folder.mkdirs();
        if (!folder.exists()) {
            LogUtil.setSaveLog(false);
            LogUtil.d("write2Sd : " + folder.getAbsolutePath() + " mkdirs failed !");
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try (FileWriter writer = new FileWriter(f, true)) {
            writer.write(format.format(System.currentTimeMillis()) + "=======" + s + "\n");
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long getOffset(String file) {
        try {
            net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile(file);
            String fileName = "payload.bin";
            FileHeader fileHeader = zipFile.getFileHeader(fileName);
            if (fileHeader == null) {
                return 0;
            }
            long offset = 30 + fileHeader.getOffsetLocalHeader() + fileName.length() + fileHeader.getExtraFieldLength();
            LogUtil.d("getOffset=" + offset);
            return offset;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static List<String> getHeaderValue(String zipFile) {
        LogUtil.d("zipFile=" + zipFile + " , " + new File(zipFile).exists());
        List<String> valueList = new ArrayList<>();
        ZipFile file = null;
        BufferedInputStream stream = null;
        BufferedReader reader = null;
        try {
            file = new ZipFile(zipFile);
            ZipEntry zipEntry = file.getEntry("payload_properties.txt");
            if (zipEntry != null) {
                stream = new BufferedInputStream(file.getInputStream(zipEntry));
                reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
                String line;
                while (!TextUtils.isEmpty(line = reader.readLine())) {
                    LogUtil.d("getHeaderValue,line=" + line);
                    valueList.add(line);
                }
            }
        } catch (IOException e) {
            LogUtil.d("upZipFile:IOException = " + e.toString());
            return valueList;
        } finally {
            try {
                if (reader != null) reader.close();
                if (stream != null) stream.close();
                if (file != null) file.close();
            } catch (IOException ignored) {

            }
        }
        LogUtil.d("getHeaderValue : " + valueList);
        return valueList;

    }

    public static void writeByteData(String filename, byte[] data) {
        FileOutputStream outputStream = null;
        try {
            File file = new File(filename);
            if (!file.exists()) file.createNewFile();
            outputStream = new FileOutputStream(file);
            outputStream.write(data);
        } catch (Exception e) {
            LogUtil.d("writeByteData : Exception = " + e.toString());
        } finally {
            try {
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {

            }
        }
    }

    public static boolean writeInternalFile(Context context, String filename, String content) {
        try (FileOutputStream fileOutputStream = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            byte[] bytes = content.getBytes();
            fileOutputStream.write(bytes);
        } catch (Exception e) {
            LogUtil.d("writeInternalFile : Exception = " + e.toString());
            return false;
        }
        return true;
    }

    public static String readInternalFile(Context context, String fileName) {
        String result = "";
        try (FileInputStream fileInputStream = context.openFileInput(fileName)) {
            int length = fileInputStream.available();
            byte[] buffer = new byte[length];
            fileInputStream.read(buffer);
            result = new String(buffer, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LogUtil.d("readInternalFile,Exception e="+e.getMessage());
        }
        return result;
    }

    public static void delFolder(String folderPath) {
        delAllFile(folderPath);
        File myFilePath = new File(folderPath);
        try {
            if (myFilePath.exists()) {
                myFilePath.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private static void delAllFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        if (!file.isDirectory()) {
            return;
        }
        String[] tempList = file.list();
        File temp;
        if (tempList != null) {
            for (String s : tempList) {
                if (path.endsWith(File.separator)) {
                    temp = new File(path + s);
                } else {
                    temp = new File(path + File.separator + s);
                }
                if (temp.isFile()) {
                    temp.delete();
                }
                if (temp.isDirectory()) {
                    delAllFile(path + "/" + s);
                    delFolder(path + "/" + s);
                }
            }
        }
    }

    public static long getFileSize(String path) {

        File file = new File(path);
        return (file.exists() && file.isFile()) ? file.length() : 0;

    }

    public static void deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            file.delete();
        }
    }

    public static boolean renameFile(String fromfile, String toFile) {
        File toBeRenamed = new File(fromfile);
        if (!toBeRenamed.exists() || toBeRenamed.isDirectory()) {
            return false;
        }
        File newFile = new File(toFile);
        return toBeRenamed.renameTo(newFile);
    }

    public static boolean mergeFile(String partFileList, String saveFileName) {
        if (partFileList.equals(saveFileName)) {
            LogUtil.d("partFileList == saveFileName");
            return true;
        }
        if (!new File(saveFileName).exists() || !new File(partFileList).exists()) {
            LogUtil.d("saveFileName or partFileList is not exist");
            return false;
        }
        boolean isMergeOk = false;
        FileChannel in = null;
        FileChannel out = null;
        FileInputStream inStream = null;
        FileOutputStream outStream = null;
        int i = 0;
        while (!isMergeOk) {
            try {
                inStream = new FileInputStream(partFileList);
                outStream = new FileOutputStream(saveFileName, true);
                in = inStream.getChannel();
                out = outStream.getChannel();
                in.transferTo(0, in.size(), out);
                isMergeOk = true;
            } catch (Exception e) {
                LogUtil.d("Exception e=" + e.toString());
                e.printStackTrace();
            } finally {
                try {
                    if (inStream != null) inStream.close();
                    if (in != null) in.close();
                    if (outStream != null) outStream.close();
                    if (out != null) out.close();
                } catch (Exception ignored) {

                }
            }
        }
        deleteFile(partFileList);
        LogUtil.d("isMergeOk = " + isMergeOk);
        return isMergeOk;
    }

    public static boolean copy(String oldPath, String newPath, Boolean isDelete) {
        LogUtil.d("copy, oldPath = " + oldPath);
        LogUtil.d("copy, newPath = " + newPath);
        boolean isCopyOk = false;
        try {
            int bytesum = 0;
            int byteread = 0;
            File toFile = new File(newPath);
            File oldFile = new File(oldPath);
            if (toFile.exists()) {
                toFile.delete();
            } else {
                LogUtil.d("copy, newPath = " + newPath + " is not exist!");
            }
            if (oldFile.exists()) {
                InputStream inStream = new FileInputStream(oldPath);
                FileOutputStream fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1024 * 32];
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread;
                    fs.write(buffer, 0, byteread);
                }
                inStream.close();
                fs.close();
                isCopyOk = true;
                if (isDelete) {
                    LogUtil.d("copy success to delete" + oldFile.delete());
                }
            } else {
                LogUtil.d("copy, oldPath = " + oldPath + " is not exist!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.d("copy, Exception" + e.toString());
        }
        LogUtil.d("copy, isOk " + isCopyOk);
        return isCopyOk;
    }

    /**
     * modify by raise.yang 2016/05/20
     * 适配各种机型，解决显示过长问题
     * 取合适大小的前3位有效数字：
     * 例如：原格式 345.12MB -> 345MB
     * 原格式5.12MB -> 5.12MB
     *
     * @param size
     * @return
     */
    public static String convertFileSize(long size) {
        float result = size;
        String suffix = "B";
        if (result > 1024) {
            suffix = "KB";
            result = result / 1024;
        }
        if (result > 1024) {
            suffix = "MB";
            result = result / 1024;
        }
        if (result > 1024) {
            suffix = "GB";
            result = result / 1024;
        }
        if (result > 1024) {
            suffix = "TB";
            result = result / 1024;
        }

        final String roundFormat;
        if (result < 10) {
            roundFormat = "%.2f";
        } else if (result < 100) {
            roundFormat = "%.1f";
        } else {
            roundFormat = "%.0f";
        }
        return String.format(roundFormat + suffix, result);
    }


    private static String byteArrayToHexString(byte[] bytearray) {
        StringBuilder strDigest = new StringBuilder();
        for (byte b : bytearray) {
            strDigest.append(byteToHexString(b));
        }
        return strDigest.toString();
    }

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


    /**
     * unzip packages
     * 将zipFile文件解压到folderPath目录下.
     *
     * @throws IOException
     * @throws Exception
     */
    @SuppressWarnings("resource")
    public static boolean unZipFile(File zipFile, String folderPath) {
        BufferedOutputStream os = null;
        BufferedInputStream is = null;
        ZipFile zfile = null;
        String szName = "";
        try {
            zfile = new ZipFile(zipFile);
            Enumeration zList = zfile.entries();
            ZipEntry ze = null;
            byte[] buf = new byte[1024];
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                szName = ze.getName();
                if (TextUtils.isEmpty(szName)) {
                    LogUtil.d("upZipFile:: zipEntry is null or zipEntry.getName is empty !");
                    return false;
                }
                LogUtil.d("upZipFile:: ze.getName() = " + ze.getName());
                if (ze.isDirectory()) {
                    szName = szName.substring(0, szName.length() - 1);
                    File f = new File(folderPath + File.separator + szName);
                    if (!f.exists()) {
                        f.mkdirs();
                    }
                } else {
                    File file = new File(folderPath + File.separator + szName);
                    LogUtil.d("upZipFile::file = " + file.getPath());
                    File parent = new File(file.getParent());
                    if (!parent.exists()) {
                        parent.mkdirs();
                    }
                    try {
                        if (!file.exists()) {
                            file.createNewFile();
                        }
                    } catch (IOException newFileException) {
                        LogUtil.d("upZipFile::createNewFile  exception  = " + newFileException.toString());
                        newFileException.printStackTrace();
                    }
                    os = new BufferedOutputStream(new FileOutputStream(file));
                    is = new BufferedInputStream(zfile.getInputStream(ze));
                    int readLen;
                    while ((readLen = is.read(buf, 0, 1024)) != -1) {
                        os.write(buf, 0, readLen);
                    }
                    LogUtil.d("upZipFile::finish the path: " + file.getPath());
                    is.close();
                    os.close();
                }
            }
            zfile.close();
            LogUtil.d("upZipFile::finish !");
            return true;
        } catch (ZipException zipex) {
            LogUtil.d("upZipFile::zipex = " + zipex.toString());
            zipex.printStackTrace();
            return false;
        } catch (IOException ioex) {
            LogUtil.d("upZipFile::ioex = " + ioex.toString());
            ioex.printStackTrace();
            return false;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
                if (zfile != null) {
                    zfile.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }

    }

    public static String getFileMD5(String file) {
        FileInputStream fis = null;
        StringBuilder buf = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            fis = new FileInputStream(file);
            byte[] buffer = new byte[1024 * 256];
            int length = -1;
            long s = System.currentTimeMillis();
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
                    buf.append("0");
                }
                buf.append(md5s);
            }
            return buf.toString();
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

    public static String getMD5sum(String file) {
        LogUtil.d("getMD5sum");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[64];
            int length = -1;
            length = fis.read(buffer);
            LogUtil.d("getMD5sum 1, length = " + length);
            if (length > 32) {
                length = 32;
            }
            LogUtil.d("getMD5sum 2, length = " + length);
            return new String(buffer, 0, length);
        } catch (Exception e) {
            e.printStackTrace();
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


    public static void writeMD5File(Context ctx, String path) {
        LogUtil.d("writeMD5File");
        int i = 0;
        File fromFile = new File(path + "/" + "md5sum");
        if (!fromFile.exists()) {
            return;
        }
        FileInputStream fosfrom = null;
        RandomAccessFile out = null;
        try {

            File file = new File(ctx.getFilesDir() + "/" + "md5sum");
            if (file.exists()) {
                LogUtil.d("writeMD5File, del exists md5sum file");
                file.delete();
            }
            out = new RandomAccessFile(file, "rws");
            fosfrom = new FileInputStream(fromFile);
            int rc;
            byte[] buff = new byte[4096];
            while ((rc = fosfrom.read(buff, 0, 4096)) > 0) {
                out.write(buff, 0, rc);
                i += rc;
            }
            out.close();
            fosfrom.close();
            LogUtil.d("writeMD5File, finish, i = " + i + "bytes");
        } catch (Exception e) {
            LogUtil.d("writeMD5File, Exception" + e);
        } finally {
            try {
                if (out != null)
                    out.close();
            } catch (Exception ignored) {

            }
            try {
                if (fosfrom != null)
                    fosfrom.close();
            } catch (Exception ignored) {

            }
        }
    }


    public static String readFileSdcardFile(String fileName) {
        String content = null;
        try {
            File file = new File(fileName);
            if (!file.exists()) return null;
            FileInputStream fileInputStream = new FileInputStream(file);
            int length = fileInputStream.available();
            byte[] buffer = new byte[length];
            fileInputStream.read(buffer);
            content = new String(buffer, StandardCharsets.UTF_8);
            fileInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return content;
    }

    public static boolean isZipFile(String fileName) throws IOException {
        String zipHeader = "504B0304";
        try (RandomAccessFile raf = new RandomAccessFile(fileName, "r")) {
            raf.seek(0);

            byte[] buf = new byte[4];
            int hasRead = raf.read(buf, 0, buf.length);
            String header = byteArrayToHexString(buf);
            if (hasRead > 0 && header.equalsIgnoreCase(zipHeader)) {
                return true;
            }
        }
        return false;
    }

    public static void modifyFileContent(String fileName, long pos, String Content) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
            raf.seek(pos);
            raf.write(Content.getBytes());
        }
    }

    public static boolean hasWritePermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasReadPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return true;
        return context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isExistTraceFile(Context context) {
        try {
            String logSwitchFileName = "/trace.trace";
            String path = context.getExternalFilesDir(null) + logSwitchFileName;
            if (isFilExist(path)) {
                return true;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    path = Environment.getExternalStorageDirectory() + logSwitchFileName;
                    return isFilExist(path);
                }
            } else {
                path = Environment.getExternalStorageDirectory() + logSwitchFileName;
                return isFilExist(path);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isFilExist(String path) {
        return new File(path).exists();
    }

}
