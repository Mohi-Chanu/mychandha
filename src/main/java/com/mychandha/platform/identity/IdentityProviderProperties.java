package com.mychandha.platform.identity;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "mychandha.identity")
public record IdentityProviderProperties(
        @NotBlank String provider,
        @NotBlank String issuer,
        @NotBlank String jwkSetUri,
        @NotBlank String audience) {
}
