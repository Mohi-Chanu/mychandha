package com.mychandha.platform.tenancy;

import com.mychandha.platform.security.RequiresPermission;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/platform")
public class OrganizationAccessController {

    @GetMapping("/access")
    @RequiresPermission("platform.access")
    public AccessResponse access(@PathVariable UUID organizationId) {
        UUID contextOrganization = OrganizationContext.require().organizationId();
        if (!organizationId.equals(contextOrganization)) {
            throw new OrganizationAccessDeniedException();
        }
        return new AccessResponse(contextOrganization, Set.of("platform.access"));
    }

    public record AccessResponse(UUID organizationId, Set<String> confirmedPermissions) {

        public AccessResponse {
            confirmedPermissions = Set.copyOf(confirmedPermissions);
        }
    }
}
