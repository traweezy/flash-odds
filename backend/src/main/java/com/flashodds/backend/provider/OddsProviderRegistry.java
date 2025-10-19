package com.flashodds.backend.provider;

import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class OddsProviderRegistry {

    private final Map<String, OddsProvider> providers;

    public OddsProviderRegistry(java.util.List<OddsProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> provider.name().toLowerCase(Locale.ROOT),
                        provider -> provider));
    }

    public OddsProvider get(String name) {
        var key = name.toLowerCase(Locale.ROOT);
        var provider = providers.get(key);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown odds provider: " + name);
        }
        return provider;
    }

    public Map<String, OddsProvider> all() {
        return providers;
    }
}
