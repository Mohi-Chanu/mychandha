package com.mychandha.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class TrustedProxyClientAddressResolverTest {

    @Test
    void ignoresForwardedHeaderFromUntrustedRemoteAddress() {
        TrustedProxyClientAddressResolver resolver = resolver();
        MockHttpServletRequest request = request("203.0.113.9");
        request.addHeader(TrustedProxyClientAddressResolver.FORWARDED_FOR, "198.51.100.4");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.9");
    }

    @Test
    void usesOnlyTerminalHopFromTrustedProxy() {
        TrustedProxyClientAddressResolver resolver = resolver();
        MockHttpServletRequest request = request("10.2.3.4");
        request.addHeader(
                TrustedProxyClientAddressResolver.FORWARDED_FOR,
                "192.0.2.99, 198.51.100.7");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void malformedOrDuplicateForwardingFallsBackToTrustedRemote() {
        TrustedProxyClientAddressResolver resolver = resolver();
        MockHttpServletRequest malformed = request("10.2.3.4");
        malformed.addHeader(TrustedProxyClientAddressResolver.FORWARDED_FOR, "attacker.example");
        MockHttpServletRequest duplicate = request("10.2.3.4");
        duplicate.addHeader(
                TrustedProxyClientAddressResolver.FORWARDED_FOR,
                List.of("198.51.100.7", "198.51.100.8"));

        assertThat(resolver.resolve(malformed)).isEqualTo("10.2.3.4");
        assertThat(resolver.resolve(duplicate)).isEqualTo("10.2.3.4");
    }

    @Test
    void canonicalizesIpv4MappedIpv6Address() {
        TrustedProxyClientAddressResolver resolver = resolver();
        MockHttpServletRequest request = request("10.2.3.4");
        request.addHeader(
                TrustedProxyClientAddressResolver.FORWARDED_FOR,
                "::ffff:192.0.2.10");

        assertThat(resolver.resolve(request)).isEqualTo("192.0.2.10");
    }

    private TrustedProxyClientAddressResolver resolver() {
        return new TrustedProxyClientAddressResolver(
                RateLimitTestSupport.proxyProperties(List.of("10.0.0.0/8")));
    }

    private MockHttpServletRequest request(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
