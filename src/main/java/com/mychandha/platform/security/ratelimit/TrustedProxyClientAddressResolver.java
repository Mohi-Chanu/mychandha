package com.mychandha.platform.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"api", "local", "test"})
final class TrustedProxyClientAddressResolver {

    static final String FORWARDED_FOR = "X-Forwarded-For";
    private static final int MAX_HEADER_LENGTH = 512;
    private static final int MAX_HOPS = 10;

    private final boolean trustForwarded;
    private final List<IpNetwork> trustedProxyNetworks;

    TrustedProxyClientAddressResolver(RateLimitProperties properties) {
        trustForwarded = properties.trustForwarded();
        trustedProxyNetworks = properties.trustedProxyCidrs().stream()
                .map(IpNetwork::parse)
                .toList();
    }

    String resolve(HttpServletRequest request) {
        String remoteAddress =
                IpNetwork.canonical(request.getRemoteAddr()).orElse("unknown");
        if (!trustForwarded
                || trustedProxyNetworks.stream().noneMatch(network -> network.contains(remoteAddress))) {
            return remoteAddress;
        }

        List<String> headers = Collections.list(request.getHeaders(FORWARDED_FOR));
        if (headers.size() != 1) {
            return remoteAddress;
        }
        String forwarded = headers.getFirst();
        if (forwarded == null || forwarded.length() > MAX_HEADER_LENGTH) {
            return remoteAddress;
        }
        String[] hops = forwarded.split(",", -1);
        if (hops.length == 0 || hops.length > MAX_HOPS) {
            return remoteAddress;
        }
        return IpNetwork.canonical(hops[hops.length - 1].trim()).orElse(remoteAddress);
    }

    boolean isForwardedBoundaryConfigured() {
        return trustForwarded && !trustedProxyNetworks.isEmpty();
    }
}
