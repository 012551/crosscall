package com.adups.fota.utils;

import android.text.TextUtils;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class EncryptUtil {

    private final static String DES = "DES";
    private static final char[] hexDigits =
            {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /**
     * 解密
     *
     * @param src 数据源
     * @param key 密钥，长度必须是8的倍数
     * @return 返回解密后的原始数据
     * @throws Exception
     */
    private static byte[] desDecrypt(byte[] src, byte[] key) throws Exception {
        SecureRandom sr = new SecureRandom();
        DESKeySpec dks = new DESKeySpec(key);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(DES);
        SecretKey securekey = keyFactory.generateSecret(dks);
        Cipher cipher = Cipher.getInstance(DES);
        cipher.init(Cipher.DECRYPT_MODE, securekey, sr);
        return cipher.doFinal(src);
    }

    /**
     * 密码解密
     *
     * @param data
     * @return
     * @throws Exception
     */
    public static String desDecode(String data, String key) throws Exception {
        return new String(desDecrypt(desHex2byte(data.getBytes()),
                (key + "s+-=6").getBytes()));
    }

    private static byte[] desHex2byte(byte[] b) {
        if ((b.length % 2) != 0)
            throw new IllegalArgumentException("长度不是偶数");
        byte[] b2 = new byte[b.length / 2];
        for (int n = 0; n < b.length; n += 2) {
            String item = new String(b, n, 2);
            b2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        return b2;
    }

    public static String decodeRoValue(String value) {
        String character = "AEVA";
        if (value.startsWith(character)) {
            String replaceValue = value.replaceAll(character, "");
            if (!TextUtils.isEmpty(replaceValue))
                return new String(Base64.decode(replaceValue, Base64.URL_SAFE));
            else
                return "";
        }
        return value;
    }

    private static String byte2hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        String s;
        for (byte b : bytes) {
            s = (Integer.toHexString(b & 0XFF));
            if (s.length() == 1)
                builder.append("0").append(s);
            else
                builder.append(s);
        }
        return builder.toString().toUpperCase();
    }

    // 编码方式产生key与服务器方法一致，切勿修改此方法。
    public static String encode(String strInput) {
        byte[] buf = null;
        byte[] data = strInput.getBytes();
        int keyIndex = (int) (Math.random() * 15);
        int keyLen = 3 + (int) (Math.random() * 12);
        byte[] keys = generateKey(keyLen);
        int head = keyIndex << 4 | keyLen;
        byte[] encodeBodies = encodeBody(keys, data);
        byte[] encodedKeys = encodeKey(keys);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(head);
            if (keyIndex > 0) {
                byte[] temp = new byte[keyIndex];
                temp[0] = 8;
                dos.write(temp);
            }
            dos.write(encodedKeys);
            dos.write(encodeBodies);
            buf = baos.toByteArray();

            dos.close();
            baos.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return byte2hex(buf);
    }

    private static byte[] generateKey(int keyLen) {
        byte[] keys = new byte[keyLen];
        for (int i = 0; i < keyLen; i++) {
            keys[i] = (byte) (Math.random() * 255);
        }
        return keys;
    }

    private static byte[] encodeKey(byte[] bys) {
        int size = bys.length;
        byte[] temp = new byte[size];
        for (int i = 0; i < size; i++) {
            temp[i] = (byte) (((bys[i] & 0xff) >> 5) | ((bys[i] & 0xff) << 3));
        }
        return temp;
    }

    private static byte[] encodeBody(byte[] keys, byte[] bodys) {
        int bodySize = bodys.length;
        int keySize = keys.length;
        byte[] temp = new byte[bodySize];
        for (int i = 0, j = 0; i < bodySize; i++) {
            temp[i] = (byte) ((bodys[i] & 0xff) ^ (keys[j] & 0xff));
            j++;
            if (j == keySize) {
                j = 0;
            }
        }
        return temp;
    }

    private static String toHexString(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(hexDigits[(b >> 4) & 0x0F]);
            hex.append(hexDigits[b & 0x0F]);
        }
        return hex.toString();
    }


    public static String md5Encode(String string) {
        byte[] encodeBytes;
        try {
            encodeBytes = MessageDigest.getInstance("MD5").digest(string.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException neverHappened) {
            return null;
        }
        return toHexString(encodeBytes);
    }

    public static String getMD5(String info) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(info.getBytes(StandardCharsets.UTF_8));
            byte[] md5Array = md5.digest();
            return bytesToHex1(md5Array);
        } catch (Exception e) {
            return "";
        }
    }

    private static String bytesToHex1(byte[] md5Array) {
        StringBuilder strBuilder = new StringBuilder();
        for (byte array : md5Array) {
            int temp = 0xff & array;
            String hexString = Integer.toHexString(temp);
            if (hexString.length() == 1) {
                strBuilder.append("0").append(hexString);
            } else {
                strBuilder.append(hexString);
            }
        }
        return strBuilder.toString();
    }

    public static String sha256(String strSrc) {
        MessageDigest md = null;
        String strDes = null;
        byte[] bt = strSrc.getBytes();
        try {
            md = MessageDigest.getInstance("SHA-256");
            md.update(bt);
            strDes = bytes2Hex(md.digest()); // to HexString
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return strDes;
    }

    public static String sha256File(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        FileInputStream in = null;
        MessageDigest messagedigest;
        try {
            messagedigest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[1024 * 100];
            int len = 0;
            in = new FileInputStream(file);
            while ((len = in.read(buffer)) > 0) {
                //该对象通过使用 update（）方法处理数据
                messagedigest.update(buffer, 0, len);
            }
            return byteArrayToHexString(messagedigest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    // 将字节转换为十六进制字符串
    private static String byteToHexString(byte ib) {
        char[] Digit = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        char[] ob = new char[2];
        ob[0] = Digit[(ib >>> 4) & 0X0F];
        ob[1] = Digit[ib & 0X0F];
        return new String(ob);
    }

    // 将字节数组转换为十六进制字符串
    private static String byteArrayToHexString(byte[] byteArray) {
        StringBuilder strDigest = new StringBuilder();
        for (byte b : byteArray) {
            strDigest.append(byteToHexString(b));
        }
        return strDigest.toString();
    }

    private static String bytes2Hex(byte[] bts) {
        StringBuilder des = new StringBuilder();
        String tmp;
        for (byte bt : bts) {
            tmp = (Integer.toHexString(bt & 0xFF));
            if (tmp.length() == 1) {
                des.append("0");
            }
            des.append(tmp);
        }
        return des.toString();
    }

}
