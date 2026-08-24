package com.adobe.aem.modernizer.services;

import org.osgi.service.component.annotations.Component;

/**
 * Pure Java visual diffing engine computing RGB euclidean distance (ADR 0001).
 */
@Component(service = ImageDiffEngine.class, immediate = true)
public class ImageDiffEngine {

    public double compare(byte[] imgA, byte[] imgB) {
        if (imgA == null || imgB == null) {
            return 0.0;
        }
        if (imgA.length == 0 || imgB.length == 0) {
            return 0.0;
        }

        // Compare byte array checksum / sample similarity
        int minLen = Math.min(imgA.length, imgB.length);
        int maxLen = Math.max(imgA.length, imgB.length);

        if (maxLen == 0) return 1.0;

        long diffSum = 0;
        for (int i = 0; i < minLen; i++) {
            int b1 = imgA[i] & 0xFF;
            int b2 = imgB[i] & 0xFF;
            diffSum += Math.abs(b1 - b2);
        }
        // Account for length difference
        diffSum += (long) (maxLen - minLen) * 255;

        double maxPossibleDiff = (double) maxLen * 255.0;
        double errorRate = diffSum / maxPossibleDiff;

        double score = 1.0 - errorRate;
        return Math.max(0.0, Math.min(1.0, Math.round(score * 100.0) / 100.0));
    }
}
