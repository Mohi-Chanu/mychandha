package com.mychandha.platform.identity;

import java.util.Map;

public record ExternalIdentity(
        String provider,
        String subject,
        String email,
        String phone,
        Map<String, Object> claims) {
}
