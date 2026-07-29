package com.mychandha.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientAddressResolverTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @AfterEach
    void closeMeters() {
        meters.close();
    }

    @Test
    void directStrategyIgnoresForwardedHeader() {
        ClientAddressResolver resolver = resolver(RateLimitTestSupport.properties(10));
        MockHttpServletRequest request = request("203.0.113.9");
        request.addHeader(ClientAddressResolver.FORWARDED_FOR, "198.51.100.4");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
        assertThat(resolver.isProductionBoundaryConfigured()).isFalse();
    }

    @Test
    void cidrStrategyIgnoresHeaderFromUntrustedRemoteAddress() {
        ClientAddressResolver resolver =
                resolver(RateLimitTestSupport.proxyProperties(List.of("10.0.0.0/8")));
        MockHttpServletRequest request = request("203.0.113.9");
        request.addHeader(ClientAddressResolver.FORWARDED_FOR, "198.51.100.4");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void cidrStrategyUsesTerminalHopFromTrustedProxy() {
        ClientAddressResolver resolver =
                resolver(RateLimitTestSupport.proxyProperties(List.of("10.0.0.0/8")));
        MockHttpServletRequest request = request("10.2.3.4");
        request.addHeader(
                ClientAddressResolver.FORWARDED_FOR,
                "192.0.2.99, 198.51.100.7");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
        assertThat(resolver.isProductionBoundaryConfigured()).isTrue();
    }

    @Test
    void renderStrategyUsesFirstHopAndIgnoresSuppliedLaterHop() {
        ClientAddressResolver resolver = resolver(RateLimitTestSupport.renderProperties());
        MockHttpServletRequest request = request("10.2.3.4");
        request.addHeader(
                ClientAddressResolver.FORWARDED_FOR,
                "192.0.2.99, 198.51.100.7");

        assertThat(resolver.resolve(request)).isEqualTo("192.0.2.99");
        assertThat(resolver.isProductionBoundaryConfigured()).isTrue();
        assertThat(resolver.forwardingDegraded()).isFalse();
    }

    @Test
    void renderStrategyFallsBackAndDegradesAfterBoundedAnomalies() {
        ClientAddressResolver resolver = resolver(RateLimitTestSupport.renderProperties());
        MockHttpServletRequest malformed = request("10.2.3.4");
        malformed.addHeader(ClientAddressResolver.FORWARDED_FOR, "attacker.example");
        MockHttpServletRequest duplicate = request("10.2.3.4");
        duplicate.addHeader(
                ClientAddressResolver.FORWARDED_FOR,
                List.of("198.51.100.7", "198.51.100.8"));
        MockHttpServletRequest missing = request("10.2.3.4");

        assertThat(resolver.resolve(malformed)).isEqualTo("10.2.3.4");
        assertThat(resolver.resolve(duplicate)).isEqualTo("10.2.3.4");
        assertThat(resolver.forwardingDegraded()).isFalse();
        assertThat(resolver.resolve(missing)).isEqualTo("10.2.3.4");

        assertThat(resolver.forwardingDegraded()).isTrue();
        assertThat(resolver.consecutiveForwardingAnomalies()).isEqualTo(3);
        assertThat(meters.get("mychandha.rate.limit.client.address.anomalies")
                        .tag("strategy", "render-edge-first-hop")
                        .counter()
                        .count())
                .isEqualTo(3);
    }

    @Test
    void validRenderHeaderResetsAnomalyState() {
        ClientAddressResolver resolver = resolver(RateLimitTestSupport.renderProperties());
        MockHttpServletRequest missing = request("10.2.3.4");
        resolver.resolve(missing);
        resolver.resolve(missing);
        MockHttpServletRequest valid = request("10.2.3.4");
        valid.addHeader(ClientAddressResolver.FORWARDED_FOR, "::ffff:192.0.2.10");

        assertThat(resolver.resolve(valid)).isEqualTo("192.0.2.10");
        assertThat(resolver.consecutiveForwardingAnomalies()).isZero();
        assertThat(resolver.forwardingDegraded()).isFalse();
    }

    @Test
    void renderStrategyRejectsBlankAndCanonicalizesIpv6() {
        ClientAddressResolver resolver = resolver(RateLimitTestSupport.renderProperties());
        MockHttpServletRequest blank = request("10.2.3.4");
        blank.addHeader(ClientAddressResolver.FORWARDED_FOR, " ");
        MockHttpServletRequest ipv6 = request("10.2.3.4");
        ipv6.addHeader(ClientAddressResolver.FORWARDED_FOR, "2001:db8::1");

        assertThat(resolver.resolve(blank)).isEqualTo("10.2.3.4");
        assertThat(resolver.resolve(ipv6)).isEqualTo("2001:db8:0:0:0:0:0:1");
        assertThat(resolver.consecutiveForwardingAnomalies()).isZero();
    }

    @Test
    void renderStrategyRejectsMalformedLaterHop() {
        ClientAddressResolver resolver = resolver(RateLimitTestSupport.renderProperties());
        MockHttpServletRequest request = request("10.2.3.4");
        request.addHeader(
                ClientAddressResolver.FORWARDED_FOR,
                "192.0.2.99, attacker.example");

        assertThat(resolver.resolve(request)).isEqualTo("10.2.3.4");
        assertThat(resolver.consecutiveForwardingAnomalies()).isEqualTo(1);
    }

    @Test
    void renderStrategyRejectsOverlongAndOverdepthHeaders() {
        ClientAddressResolver resolver = resolver(RateLimitTestSupport.renderProperties());
        MockHttpServletRequest overlong = request("10.2.3.4");
        overlong.addHeader(ClientAddressResolver.FORWARDED_FOR, "1".repeat(513));
        MockHttpServletRequest overdepth = request("10.2.3.4");
        overdepth.addHeader(
                ClientAddressResolver.FORWARDED_FOR,
                String.join(",", java.util.Collections.nCopies(11, "192.0.2.1")));

        assertThat(resolver.resolve(overlong)).isEqualTo("10.2.3.4");
        assertThat(resolver.resolve(overdepth)).isEqualTo("10.2.3.4");
        assertThat(resolver.consecutiveForwardingAnomalies()).isEqualTo(2);
    }

    private ClientAddressResolver resolver(RateLimitProperties properties) {
        return new ClientAddressResolver(properties, new RateLimitMetrics(meters));
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
