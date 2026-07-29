package com.mychandha.platform.security.ratelimit;

public enum ClientAddressStrategy {
    DIRECT("direct"),
    TRUSTED_PROXY_CIDR("trusted-proxy-cidr"),
    RENDER_EDGE_FIRST_HOP("render-edge-first-hop");

    private final String tag;

    ClientAddressStrategy(String tag) {
        this.tag = tag;
    }

    String tag() {
        return tag;
    }
}
