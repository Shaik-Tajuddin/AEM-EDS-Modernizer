package com.adobe.aem.modernizer.scopes;

import org.osgi.service.component.annotations.Component;

/**
 * Evaluates whether a given AEM path is within the project's content root and scope filter.
 */
@Component(service = ScopeEvaluator.class, immediate = true)
public class ScopeEvaluator {

    public boolean isInScope(String path, String contentRoot, String pageScope) {
        if (path == null) {
            return false;
        }

        // Must be under contentRoot if specified
        if (contentRoot != null && !contentRoot.isEmpty()) {
            String rootPrefix = contentRoot.endsWith("/") ? contentRoot : (contentRoot + "/");
            if (!path.equals(contentRoot) && !path.startsWith(rootPrefix)) {
                return false;
            }
        }

        // Must match pageScope if specified
        if (pageScope != null && !pageScope.trim().isEmpty()) {
            String cleanScope = pageScope.trim();
            if (cleanScope.endsWith("/*")) {
                String prefix = cleanScope.substring(0, cleanScope.length() - 2);
                return path.startsWith(prefix);
            } else if (cleanScope.endsWith("*")) {
                String prefix = cleanScope.substring(0, cleanScope.length() - 1);
                return path.startsWith(prefix);
            } else {
                String scopePrefix = cleanScope.endsWith("/") ? cleanScope : (cleanScope + "/");
                return path.equals(cleanScope) || path.startsWith(scopePrefix);
            }
        }

        return true;
    }
}
