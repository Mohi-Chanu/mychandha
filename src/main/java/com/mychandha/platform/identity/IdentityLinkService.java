package com.mychandha.platform.identity;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityLinkService {

    private final JdbcClient jdbc;

    public IdentityLinkService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public UUID synchronize(ExternalIdentity identity) {
        var existing = jdbc.sql("""
                        SELECT user_id
                        FROM identity.identity_link
                        WHERE provider = :provider AND external_subject = :subject
                        """)
                .param("provider", identity.provider())
                .param("subject", identity.subject())
                .query(UUID.class)
                .optional();
        if (existing.isPresent()) {
            jdbc.sql("""
                            UPDATE identity.identity_link
                            SET last_seen_at = now()
                            WHERE provider = :provider AND external_subject = :subject
                            """)
                    .param("provider", identity.provider())
                    .param("subject", identity.subject())
                    .update();
            return existing.get();
        }

        UUID candidateUserId = UUID.randomUUID();
        jdbc.sql("""
                        INSERT INTO identity.user_profile (id, email_masked, phone_masked)
                        VALUES (:id, :email, :phone)
                        """)
                .param("id", candidateUserId)
                .param("email", maskEmail(identity.email()))
                .param("phone", maskPhone(identity.phone()))
                .update();
        int linked = jdbc.sql("""
                        INSERT INTO identity.identity_link (user_id, provider, external_subject)
                        VALUES (:userId, :provider, :subject)
                        ON CONFLICT (provider, external_subject) DO NOTHING
                        """)
                .param("userId", candidateUserId)
                .param("provider", identity.provider())
                .param("subject", identity.subject())
                .update();
        if (linked == 1) {
            return candidateUserId;
        }
        jdbc.sql("DELETE FROM identity.user_profile WHERE id = :id")
                .param("id", candidateUserId)
                .update();
        return jdbc.sql("""
                        SELECT user_id FROM identity.identity_link
                        WHERE provider = :provider AND external_subject = :subject
                        """)
                .param("provider", identity.provider())
                .param("subject", identity.subject())
                .query(UUID.class)
                .single();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return null;
        }
        int at = email.indexOf('@');
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return null;
        }
        return "***" + phone.substring(phone.length() - 4);
    }
}
