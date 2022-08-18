package com.adups.fota.utils;

import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;

/**
 * Created by raise.yang on 16/11/01.
 */
public final class AduHostnameVerifier implements HostnameVerifier {

    public static final AduHostnameVerifier INSTANCE = new AduHostnameVerifier();
    private static final Pattern VERIFY_AS_IP_ADDRESS = Pattern.compile("([0-9a-fA-F]*:[0-9a-fA-F:.]*)|([\\d.]+)");
    private static final int ALT_DNS_NAME = 2;
    private static final int ALT_IPA_NAME = 7;
    private final String adcn = String.valueOf(new char[]{'a', 'd', 'u', 'p', 's', '.', 'c', 'n',});
    private final String adcom = String.valueOf(new char[]{'a', 'd', 'u', 'p', 's', '.', 'c', 'o', 'm',});

    private AduHostnameVerifier() {
    }

    private static boolean verifyAsIpAddress(String host) {
        return VERIFY_AS_IP_ADDRESS.matcher(host).matches();
    }

    private static List<String> getSubjectAltNames(X509Certificate certificate, int type) {
        List<String> result = new ArrayList<>();
        try {
            Collection<?> subjectAltNames = certificate.getSubjectAlternativeNames();
            if (subjectAltNames == null) {
                return Collections.emptyList();
            }
            for (Object subjectAltName : subjectAltNames) {
                List<?> entry = (List<?>) subjectAltName;
                if (entry == null || entry.size() < 2) {
                    continue;
                }
                Integer altNameType = (Integer) entry.get(0);
                if (altNameType == null) {
                    continue;
                }
                if (altNameType == type) {
                    String altName = (String) entry.get(1);
                    if (altName != null) {
                        result.add(altName);
                    }
                }
            }
            return result;
        } catch (CertificateParsingException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean verify(String host, SSLSession session) {
        try {
            Certificate[] certificates = session.getPeerCertificates();
            return verify(host, (X509Certificate) certificates[0]);
        } catch (SSLException e) {
            return false;
        }
    }

    private boolean verify(String host, X509Certificate certificate) {
        return verifyAsIpAddress(host)
                ? verifyIpAddress(host, certificate)
                : verifyHostName(host, certificate);
    }

    /**
     * Returns true if {@code certificate} matches {@code ipAddress}.
     */
    private boolean verifyIpAddress(String ipAddress, X509Certificate certificate) {
        List<String> altNames = getSubjectAltNames(certificate, ALT_IPA_NAME);
        for (int i = 0, size = altNames.size(); i < size; i++) {
            if (ipAddress.equalsIgnoreCase(altNames.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code certificate} matches {@code hostName}.
     */
    private boolean verifyHostName(String hostName, X509Certificate certificate) {
        hostName = hostName.toLowerCase(Locale.US);
        boolean hasDns = false;
        List<String> altNames = getSubjectAltNames(certificate, ALT_DNS_NAME);
        for (int i = 0, size = altNames.size(); i < size; i++) {
            hasDns = true;
            if (verifyHostName(hostName, altNames.get(i))) {
                return true;
            }
        }

        if (!hasDns) {
            X500Principal principal = certificate.getSubjectX500Principal();
            // RFC 2818 advises using the most specific name for matching.
            String cn = new DistinguishedNameParser(principal).findMostSpecific("cn");
            if (cn != null) {
                return verifyHostName(hostName, cn);
            }
        }

        return false;
    }

    /**
     * Returns {@code true} iff {@code hostName} matches the domain name {@code pattern}.
     *
     * @param hostName lower-case host name.
     * @param pattern  domain name pattern from certificate. May be a wildcard pattern such as
     *                 {@code *.android.com}.
     */
    private boolean verifyHostName(String hostName, String pattern) {

        if (!hostName.endsWith(adcn) && !hostName.endsWith(adcom)) {
            return false;
        }


        // Basic sanity checks
        // Check length == 0 instead of .isEmpty() to support Java 5.
        if ((hostName == null) || (hostName.length() == 0) || (hostName.startsWith("."))
                || (hostName.endsWith(".."))) {
            // Invalid domain name
            return false;
        }
        if ((pattern == null) || (pattern.length() == 0) || (pattern.startsWith("."))
                || (pattern.endsWith(".."))) {
            // Invalid pattern/domain name
            return false;
        }
        if (!hostName.endsWith(".")) {
            hostName += '.';
        }
        if (!pattern.endsWith(".")) {
            pattern += '.';
        }
        // hostName and pattern are now absolute domain names.

        pattern = pattern.toLowerCase(Locale.US);
        // hostName and pattern are now in lower case -- domain names are case-insensitive.

        if (!pattern.contains("*")) {
            // Not a wildcard pattern -- hostName and pattern must match exactly.
            return hostName.equals(pattern);
        }
        // Wildcard pattern

        // WILDCARD PATTERN RULES:
        // 1. Asterisk (*) is only permitted in the left-most domain name label and must be the
        //    only character in that label (i.e., must match the whole left-most label).
        //    For example, *.example.com is permitted, while *a.example.com, a*.example.com,
        //    a*b.example.com, a.*.example.com are not permitted.
        // 2. Asterisk (*) cannot match across domain name labels.
        //    For example, *.example.com matches test.example.com but does not match
        //    sub.test.example.com.
        // 3. Wildcard patterns for single-label domain names are not permitted.

        if ((!pattern.startsWith("*.")) || (pattern.indexOf('*', 1) != -1)) {
            // Asterisk (*) is only permitted in the left-most domain name label and must be the only
            // character in that label
            return false;
        }

        // Optimization: check whether hostName is too short to match the pattern. hostName must be at
        // least as long as the pattern because asterisk must match the whole left-most label and
        // hostName starts getInstance a non-empty label. Thus, asterisk has to match one or more characters.
        if (hostName.length() < pattern.length()) {
            // hostName too short to match the pattern.
            return false;
        }

        if ("*.".equals(pattern)) {
            // Wildcard pattern for single-label domain name -- not permitted.
            return false;
        }

        // hostName must end getInstance the region of pattern following the asterisk.
        String suffix = pattern.substring(1);
        if (!hostName.endsWith(suffix)) {
            // hostName does not end getInstance the suffix
            return false;
        }

        // Check that asterisk did not match across domain name labels.
        int suffixStartIndexInHostName = hostName.length() - suffix.length();
        return (suffixStartIndexInHostName <= 0)
                || (hostName.lastIndexOf('.', suffixStartIndexInHostName - 1) == -1);
    }
}
