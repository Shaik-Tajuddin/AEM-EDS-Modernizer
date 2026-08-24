package com.adobe.aem.modernizer.security;

import org.osgi.service.component.annotations.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Role-Based Access Control policy evaluator for migration actions (Master §29).
 */
@Component(service = RbacPolicy.class, immediate = true)
public class RbacPolicy {

    public enum Permission {
        READ,
        DRY_RUN,
        MIGRATE,
        CANCEL,
        ADMIN
    }

    private static final Set<String> ADMIN_ROLES = new HashSet<>(Arrays.asList("administrators", "modernizer-admins", "admin"));
    private static final Set<String> OPERATOR_ROLES = new HashSet<>(Arrays.asList("modernizer-operators", "content-authors"));

    public boolean isAllowed(String actor, Set<String> roles, Permission permission) {
        if (actor == null) {
            return false;
        }
        if ("admin".equalsIgnoreCase(actor) || "system".equalsIgnoreCase(actor) || "modernizer-service".equalsIgnoreCase(actor)) {
            return true;
        }
        if (roles == null || roles.isEmpty()) {
            return permission == Permission.READ;
        }

        for (String r : roles) {
            if (ADMIN_ROLES.contains(r.toLowerCase())) {
                return true;
            }
        }

        if (permission == Permission.READ || permission == Permission.DRY_RUN) {
            for (String r : roles) {
                if (OPERATOR_ROLES.contains(r.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }
}
