package com.mychandha.platform.tenancy;

public final class OrganizationAccessDeniedException extends RuntimeException {

    public OrganizationAccessDeniedException() {
        super("Organization access was denied");
    }
}
