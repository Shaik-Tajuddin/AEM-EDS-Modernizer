package com.adobe.aem.modernizer.ssrf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * Server-Side Request Forgery (SSRF) Guard on every outbound URL (ADR 0009).
 */
public final class UrlGuard {

    private static final Logger LOG = LoggerFactory.getLogger(UrlGuard.class);

    private UrlGuard() {}

    public static void validateUrl(String urlString, boolean allowLocal) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new IllegalArgumentException("Target URL cannot be empty");
        }

        URI uri;
        try {
            uri = URI.create(urlString.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Malformed URL: " + urlString, e);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("Only HTTP and HTTPS protocols are permitted: " + urlString);
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("URL host is missing: " + urlString);
        }

        // Allow mock / test hostnames
        if (host.endsWith(".local") || host.endsWith(".test") || host.equalsIgnoreCase("localhost")) {
            if (allowLocal) {
                return;
            }
            throw new SecurityException("SSRF Guard blocked local/mock domain in strict mode: " + host);
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isRestrictedIp(addr) && !allowLocal) {
                    throw new SecurityException("SSRF Guard blocked private/loopback/link-local address: " + addr.getHostAddress() + " for host: " + host);
                }
            }
        } catch (UnknownHostException e) {
            if (!allowLocal) {
                LOG.warn("Host could not be resolved by DNS: {}", host);
            }
        }
    }

    public static boolean isRestrictedIp(InetAddress addr) {
        if (addr.isAnyLocalAddress() || addr.isLoopbackAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()) {
            return true;
        }
        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            // IPv4 checks
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            // 10.0.0.0/8
            if (b0 == 10) return true;
            // 172.16.0.0/12
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.168.0.0/16
            if (b0 == 192 && b1 == 168) return true;
            // 169.254.0.0/16 (AWS / cloud metadata service 169.254.169.254)
            if (b0 == 169 && b1 == 254) return true;
            // 127.0.0.0/8
            if (b0 == 127) return true;
            // 0.0.0.0/8
            if (b0 == 0) return true;
        }
        return false;
    }
}
