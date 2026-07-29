package com.mychandha.platform.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"api", "local", "test"})
final class ClientAddressResolver {

    static final String FORWARDED_FOR = "X-Forwarded-For";
    private static final int MAX_HEADER_LENGTH = 512;
    private static final int MAX_HOPS = 10;
    private static final int DEGRADED_ANOMALY_THRESHOLD = 3;

    private final ClientAddressStrategy strategy;
    private final List<IpNetwork> trustedProxyNetworks;
    private final RateLimitMetrics metrics;
    private final AtomicInteger consecutiveForwardingAnomalies = new AtomicInteger();

    ClientAddressResolver(RateLimitProperties properties, RateLimitMetrics metrics) {
        strategy = properties.clientAddressStrategy();
        trustedProxyNetworks = properties.trustedProxyCidrs().stream()
                .map(IpNetwork::parse)
                .toList();
        this.metrics = metrics;
    }

    String resolve(HttpServletRequest request) {
        String remoteAddress =
                IpNetwork.canonical(request.getRemoteAddr()).orElse("unknown");
        return switch (strategy) {
            case DIRECT -> remoteAddress;
            case TRUSTED_PROXY_CIDR -> resolveTrustedProxy(request, remoteAddress);
            case RENDER_EDGE_FIRST_HOP -> resolveRenderEdge(request, remoteAddress);
        };
    }

    private String resolveTrustedProxy(
            HttpServletRequest request,
            String remoteAddress) {
        if (trustedProxyNetworks.stream()
                .noneMatch(network -> network.contains(remoteAddress))) {
            return remoteAddress;
        }
        return validatedForwardedHops(request)
                .map(hops -> IpNetwork.canonical(hops[hops.length - 1].trim())
                        .orElse(remoteAddress))
                .orElse(remoteAddress);
    }

    private String resolveRenderEdge(
            HttpServletRequest request,
            String remoteAddress) {
        Optional<String[]> forwardedHops = validatedForwardedHops(request);
        if (forwardedHops.isEmpty()) {
            recordForwardingAnomaly();
            return remoteAddress;
        }
        String[] hops = forwardedHops.orElseThrow();
        for (String hop : hops) {
            if (IpNetwork.canonical(hop.trim()).isEmpty()) {
                recordForwardingAnomaly();
                return remoteAddress;
            }
        }
        Optional<String> clientAddress = IpNetwork.canonical(hops[0].trim());
        if (clientAddress.isEmpty()) {
            recordForwardingAnomaly();
            return remoteAddress;
        }
        consecutiveForwardingAnomalies.set(0);
        return clientAddress.orElseThrow();
    }

    private Optional<String[]> validatedForwardedHops(
            HttpServletRequest request) {
        List<String> headers = Collections.list(request.getHeaders(FORWARDED_FOR));
        if (headers.size() != 1) {
            return Optional.empty();
        }
        String forwarded = headers.getFirst();
        if (forwarded == null || forwarded.length() > MAX_HEADER_LENGTH) {
            return Optional.empty();
        }
        String[] hops = forwarded.split(",", -1);
        if (hops.length == 0 || hops.length > MAX_HOPS) {
            return Optional.empty();
        }
        return Optional.of(hops);
    }

    private void recordForwardingAnomaly() {
        consecutiveForwardingAnomalies.updateAndGet(
                current -> Math.min(DEGRADED_ANOMALY_THRESHOLD, current + 1));
        metrics.recordClientAddressAnomaly(strategy);
    }

    boolean isProductionBoundaryConfigured() {
        return switch (strategy) {
            case DIRECT -> false;
            case TRUSTED_PROXY_CIDR -> !trustedProxyNetworks.isEmpty();
            case RENDER_EDGE_FIRST_HOP -> trustedProxyNetworks.isEmpty();
        };
    }

    boolean forwardingDegraded() {
        return consecutiveForwardingAnomalies.get() >= DEGRADED_ANOMALY_THRESHOLD;
    }

    int consecutiveForwardingAnomalies() {
        return consecutiveForwardingAnomalies.get();
    }
}
