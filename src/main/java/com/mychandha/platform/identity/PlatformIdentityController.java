package com.mychandha.platform.identity;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
public class PlatformIdentityController {

    private final CurrentActor currentActor;
    private final IdentityLinkService identityLinks;

    public PlatformIdentityController(CurrentActor currentActor, IdentityLinkService identityLinks) {
        this.currentActor = currentActor;
        this.identityLinks = identityLinks;
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        ExternalIdentity identity = currentActor.require(authentication);
        UUID userId = identityLinks.synchronize(identity);
        return new MeResponse(userId, identity.provider(), identity.email(), identity.phone());
    }

    public record MeResponse(UUID userId, String identityProvider, String email, String phone) {
    }
}
