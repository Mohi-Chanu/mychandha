package com.mychandha.platform.security;

import com.mychandha.platform.identity.IdentityProviderProperties;
import com.mychandha.platform.security.ratelimit.ClientAddressRateLimitFilter;
import com.mychandha.platform.security.ratelimit.OrganizationRateLimitFilter;
import com.mychandha.platform.security.ratelimit.RateLimitProperties;
import com.mychandha.platform.security.ratelimit.SubjectRateLimitFilter;
import com.mychandha.platform.tenancy.OrganizationContextFilter;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.util.StringUtils;

@Configuration
@EnableMethodSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
@Profile({"api", "local", "test"})
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorrelationIdFilter correlationIdFilter,
            ClientAddressRateLimitFilter clientAddressRateLimitFilter,
            SubjectRateLimitFilter subjectRateLimitFilter,
            OrganizationContextFilter organizationContextFilter,
            OrganizationRateLimitFilter organizationRateLimitFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/prometheus").hasAuthority("SCOPE_platform.metrics")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter()))
                        .authenticationEntryPoint(authenticationEntryPoint))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER)))
                .addFilterBefore(correlationIdFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(clientAddressRateLimitFilter, CorrelationIdFilter.class)
                .addFilterAfter(subjectRateLimitFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(organizationContextFilter, SubjectRateLimitFilter.class)
                .addFilterAfter(organizationRateLimitFilter, OrganizationContextFilter.class)
                .build();
    }

    @Bean
    FilterRegistrationBean<CorrelationIdFilter> correlationIdRegistration(
            CorrelationIdFilter filter) {
        return disabledRegistration(filter);
    }

    @Bean
    FilterRegistrationBean<ClientAddressRateLimitFilter> clientAddressRateLimitRegistration(
            ClientAddressRateLimitFilter filter) {
        return disabledRegistration(filter);
    }

    @Bean
    FilterRegistrationBean<SubjectRateLimitFilter> subjectRateLimitRegistration(
            SubjectRateLimitFilter filter) {
        return disabledRegistration(filter);
    }

    @Bean
    FilterRegistrationBean<OrganizationContextFilter> organizationContextRegistration(
            OrganizationContextFilter filter) {
        return disabledRegistration(filter);
    }

    @Bean
    FilterRegistrationBean<OrganizationRateLimitFilter> organizationRateLimitRegistration(
            OrganizationRateLimitFilter filter) {
        return disabledRegistration(filter);
    }

    @Bean
    JwtDecoder jwtDecoder(IdentityProviderProperties properties) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> audience = token -> token.getAudience().contains(properties.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "Required audience is missing", null));
        OAuth2TokenValidator<Jwt> subject = token -> StringUtils.hasText(token.getSubject())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error(
                        "invalid_token", "Subject is missing", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience, subject));
        return decoder;
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> authenticationConverter() {
        return jwt -> {
            List<org.springframework.security.core.GrantedAuthority> authorities =
                    jwt.getClaimAsStringList("scope") == null
                            ? List.of()
                            : jwt.getClaimAsStringList("scope").stream()
                                    .map(scope -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                            "SCOPE_" + scope))
                                    .map(org.springframework.security.core.GrantedAuthority.class::cast)
                                    .toList();
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    private <T extends jakarta.servlet.Filter> FilterRegistrationBean<T> disabledRegistration(
            T filter) {
        FilterRegistrationBean<T> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
