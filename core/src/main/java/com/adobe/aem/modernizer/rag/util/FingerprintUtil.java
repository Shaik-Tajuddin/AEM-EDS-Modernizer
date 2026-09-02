package com.adobe.aem.modernizer.rag.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Deterministic SHA-256 fingerprinting utility for documents, normalized chunks,
 * and embedding cache keys.
 */
public final class FingerprintUtil {

    private FingerprintUtil() {
    }

    public static String sha256(String input) {
        if (input == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available in JVM", e);
        }
    }

    /**
     * Normalizes text before chunk fingerprinting by trimming and standardizing line endings.
     */
    public static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    public static String chunkFingerprint(String docId, String section, String content) {
        String normalized = normalizeText(content);
        return sha256(docId + "::" + (section != null ? section : "") + "::" + normalized);
    }
}
