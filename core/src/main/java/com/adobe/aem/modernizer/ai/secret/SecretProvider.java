package com.adobe.aem.modernizer.ai.secret;

/**
 * Resolves secret references dynamically at point-of-use (ADR 0008).
 */
public interface SecretProvider {
    String resolve(String reference);
}
