package com.mychandha.platform.security.ratelimit;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

final class IpNetwork {

    private static final Pattern IP_LITERAL = Pattern.compile("[0-9A-Fa-f:.]+");
    private final byte[] address;
    private final int prefixLength;

    private IpNetwork(byte[] address, int prefixLength) {
        this.address = address.clone();
        this.prefixLength = prefixLength;
    }

    static IpNetwork parse(String value) {
        String[] parts = value.split("/", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Trusted proxy entry must use CIDR notation");
        }
        byte[] address = parseLiteral(parts[0])
                .orElseThrow(() -> new IllegalArgumentException("Trusted proxy CIDR is invalid"));
        int prefix;
        try {
            prefix = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Trusted proxy CIDR prefix is invalid", exception);
        }
        if (prefix < 0 || prefix > address.length * Byte.SIZE) {
            throw new IllegalArgumentException("Trusted proxy CIDR prefix is out of range");
        }
        return new IpNetwork(address, prefix);
    }

    boolean contains(String value) {
        Optional<byte[]> candidate = parseLiteral(value);
        if (candidate.isEmpty() || candidate.get().length != address.length) {
            return false;
        }
        byte[] bytes = candidate.get();
        int completeBytes = prefixLength / Byte.SIZE;
        int remainingBits = prefixLength % Byte.SIZE;
        if (!Arrays.equals(
                Arrays.copyOf(address, completeBytes),
                Arrays.copyOf(bytes, completeBytes))) {
            return false;
        }
        if (remainingBits == 0) {
            return true;
        }
        int mask = 0xff << (Byte.SIZE - remainingBits);
        return (address[completeBytes] & mask) == (bytes[completeBytes] & mask);
    }

    static Optional<String> canonical(String value) {
        return parseLiteral(value).map(bytes -> {
            try {
                return InetAddress.getByAddress(bytes).getHostAddress();
            } catch (UnknownHostException exception) {
                throw new IllegalStateException("Validated IP address could not be rendered", exception);
            }
        });
    }

    private static Optional<byte[]> parseLiteral(String value) {
        if (value == null || value.isBlank() || !IP_LITERAL.matcher(value).matches()) {
            return Optional.empty();
        }
        if (value.indexOf(':') < 0) {
            return parseIpv4(value);
        }
        try {
            InetAddress address = InetAddress.getByName(value);
            return Optional.of(address.getAddress());
        } catch (UnknownHostException exception) {
            return Optional.empty();
        }
    }

    private static Optional<byte[]> parseIpv4(String value) {
        String[] octets = value.split("\\.", -1);
        if (octets.length != 4) {
            return Optional.empty();
        }
        byte[] result = new byte[4];
        for (int index = 0; index < octets.length; index++) {
            if (octets[index].isEmpty() || octets[index].length() > 3) {
                return Optional.empty();
            }
            int octet;
            try {
                octet = Integer.parseInt(octets[index]);
            } catch (NumberFormatException exception) {
                return Optional.empty();
            }
            if (octet < 0 || octet > 255) {
                return Optional.empty();
            }
            result[index] = (byte) octet;
        }
        return Optional.of(result);
    }
}
