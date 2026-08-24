package com.adobe.aem.modernizer.ai.secret;

import org.osgi.service.component.annotations.Component;

/**
 * Environment-variable based SecretProvider implementation.
 */
@Component(service = SecretProvider.class, immediate = true)
public class EnvSecretProvider implements SecretProvider {

    @Override
    public String resolve(String reference) {
        if (reference == null || reference.trim().isEmpty()) {
            return null;
        }
        String ref = reference.trim();
        if (ref.startsWith("env:")) {
            String varName = ref.substring(4);
            return System.getenv(varName);
        }
        // Direct value fallback for local development if not matching a prefix
        return ref;
    }
}
