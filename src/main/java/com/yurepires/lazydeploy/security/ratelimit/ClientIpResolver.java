package com.yurepires.lazydeploy.security.ratelimit;

import com.yurepires.lazydeploy.config.RateLimitProperties;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Resolve o endereço do cliente sem confiar automaticamente em cabeçalhos
 * enviados pelo próprio cliente.
 */
@Component
public class ClientIpResolver {

    private final RateLimitProperties.ProxyPolicy proxyPolicy;

    public ClientIpResolver(RateLimitProperties properties) {
        this.proxyPolicy = properties.proxy();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalize(request.getRemoteAddr());
        if (!proxyPolicy.trustForwardedHeaders()) {
            return valueOrUnknown(remoteAddress);
        }

        if (!isTrustedProxy(remoteAddress)) {
            return valueOrUnknown(remoteAddress);
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return valueOrUnknown(remoteAddress);
        }

        List<String> addressChain = createAddressChain(forwardedFor, remoteAddress);
        for (int index = addressChain.size() - 1; index >= 0; index--) {
            String candidate = addressChain.get(index);
            if (!isTrustedProxy(candidate)) {
                return candidate;
            }
        }

        return valueOrUnknown(remoteAddress);
    }

    private List<String> createAddressChain(String forwardedFor, String remoteAddress) {
        List<String> addresses = new ArrayList<>(Arrays.stream(forwardedFor.split(","))
                .map(this::normalize)
                .filter(value -> value != null)
                .toList());
        if (remoteAddress != null) {
            addresses.add(remoteAddress);
        }
        return addresses;
    }

    private boolean isTrustedProxy(String address) {
        if (address == null) {
            return false;
        }
        return proxyPolicy.trustedProxies().stream()
                .map(this::normalize)
                .anyMatch(address::equals);
    }

    private String normalize(String address) {
        if (address == null) {
            return null;
        }

        String normalizedAddress = address.trim();
        if (normalizedAddress.isEmpty()) {
            return null;
        }

        if (normalizedAddress.startsWith("[") && normalizedAddress.endsWith("]")) {
            return normalizedAddress.substring(1, normalizedAddress.length() - 1);
        }

        return normalizedAddress;
    }

    private String valueOrUnknown(String address) {
        if (address == null) {
            return "unknown";
        }
        return address;
    }
}
