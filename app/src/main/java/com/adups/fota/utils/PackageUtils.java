package com.adups.fota.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.text.TextUtils;
import android.util.Base64;

import com.adups.fota.BuildConfig;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class PackageUtils {

    public static int getAppVersionCode(Context context) {
        if (context != null) {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo;
            try {
                packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
                if (packageInfo != null) {
                    return packageInfo.versionCode;
                }
            } catch (NameNotFoundException ignored) {
            }
        }
        return -1;
    }

    public static String getAppVersionName(Context context) {
        if (context != null) {
            PackageManager manager = context.getPackageManager();
            PackageInfo info;
            try {
                info = manager.getPackageInfo(context.getPackageName(), 0);
                if (info != null) {
                    if (info.versionName.equalsIgnoreCase(BuildConfig.VERSION_NAME))
                        return info.versionName;
                    else
                        return BuildConfig.VERSION_NAME;
                }
            } catch (NameNotFoundException ignored) {
            }
        }
        return "0";
    }

    public static String getSignatureMd5(Context context, String packageName) {
        if (TextUtils.isEmpty(packageName)) return null;
        PackageManager mPmManager = context.getPackageManager();
        try {
            PackageInfo info = mPmManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            if (info != null) {
                GetPkgSignatureHash hash = new GetPkgSignatureHash();
                String[] certInfo = hash.getPkgSignatureMD5(info.signatures);
                if (certInfo != null && certInfo.length == 2) {
                    return certInfo[0];
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return null;
    }

    public static int getUserId(int uid) {
        int userId = uid / 100000;
        LogUtil.d("[getUserId]userId is  = " + userId);
        return userId;
    }

    private static class GetPkgSignatureHash {

        private String toHexString(byte[] bytes) {
            StringBuilder hexString = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex).append("");
            }
            return hexString.toString();
        }

        private String getMD5String(byte[] bytes) {
            try {
                MessageDigest algorithm = MessageDigest.getInstance("MD5");
                algorithm.reset();
                algorithm.update(bytes);
                return toHexString(algorithm.digest());
            } catch (NoSuchAlgorithmException e) {
                return null;
            }
        }

        /*
         * 获得apk的签名并产生和对应的md5 hash
         *
         * @param signature
         * 		从 getInstalledPackages(PackageManager.GET_SIGNATURES) 获取
         * 		PackageInfo.signature
         *
         * 	数组下标	0 	证书MD5
         *  数组下标 1	证书描述
         */
        public String[] getPkgSignatureMD5(Signature[] signature) {
            if (signature.length == 0)
                return null;
            if (signature[0] == null)
                return null;
            byte[] cert = signature[0].toByteArray();
            if (cert.length <= 0)
                return null;
            InputStream input = new ByteArrayInputStream(cert);
            CertificateFactory cf = null;
            X509Certificate c = null;
            String[] SignatureMD5 = new String[2];
            try {
                cf = CertificateFactory.getInstance("X509");
                c = (X509Certificate) cf.generateCertificate(input);
                byte[] encoded = c.getEncoded();
                String base64 = Base64.encodeToString(encoded, Base64.NO_WRAP);
                SignatureMD5[0] = getMD5String(base64.getBytes());
                SignatureMD5[1] = c.getIssuerDN().toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            return SignatureMD5;
        }
    }

}
