package com.mychandha.platform.identity;

import org.springframework.security.core.Authentication;

public interface IdentityProvider {

    String providerName();

    ExternalIdentity resolve(Authentication authentication);
}
