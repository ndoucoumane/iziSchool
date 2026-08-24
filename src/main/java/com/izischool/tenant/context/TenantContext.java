package com.izischool.tenant.context;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;
import java.util.UUID;

@Getter
@Builder
@ToString
public class TenantContext {

    private final UUID schoolId;
    private final String schoolCode;
    private final String keycloakUserId;
    private final String email;
    private final Set<String> roles;
    private final boolean superAdmin;

    private static final ThreadLocal<TenantContext> CURRENT_CONTEXT = new ThreadLocal<>();

    public static void set(TenantContext context) {
        CURRENT_CONTEXT.set(context);
    }

    public static TenantContext get() {
        return CURRENT_CONTEXT.get();
    }

    public static UUID getCurrentSchoolId() {
        TenantContext ctx = get();
        return ctx != null ? ctx.getSchoolId() : null;
    }

    public static String getCurrentKeycloakUserId() {
        TenantContext ctx = get();
        return ctx != null ? ctx.getKeycloakUserId() : null;
    }

    public static boolean isCurrentSuperAdmin() {
        TenantContext ctx = get();
        return ctx != null && ctx.isSuperAdmin();
    }

    public static void clear() {
        CURRENT_CONTEXT.remove();
    }
}
