package com.example.platform.downloader.application.provider;

import com.example.platform.downloader.domain.enums.Platform;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProviderRegistry {

    private final List<ContentProvider> providers;

    public ProviderRegistry(List<ContentProvider> providers) {
        this.providers = providers;
    }

    public ContentProvider byPlatform(Platform platform) {
        return providers.stream()
                .filter(provider -> provider.platform() == platform)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Không có provider cho platform " + platform));
    }

    public ContentProvider detect(String url) {
        return providers.stream()
                .filter(provider -> provider.supports(url))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("URL không thuộc nền tảng hỗ trợ"));
    }
}
