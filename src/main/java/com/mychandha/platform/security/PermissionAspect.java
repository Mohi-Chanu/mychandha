package com.mychandha.platform.security;

import com.mychandha.platform.tenancy.OrganizationAccessDeniedException;
import com.mychandha.platform.tenancy.OrganizationContext;
import com.mychandha.platform.tenancy.TenantAccessService;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Profile({"api", "local", "test"})
public class PermissionAspect {

    private final TenantAccessService tenantAccess;

    public PermissionAspect(TenantAccessService tenantAccess) {
        this.tenantAccess = tenantAccess;
    }

    @Before("@annotation(requiresPermission)")
    public void enforce(RequiresPermission requiresPermission) {
        OrganizationContext.require();
        if (!tenantAccess.hasPermission(requiresPermission.value())) {
            throw new OrganizationAccessDeniedException();
        }
    }
}
