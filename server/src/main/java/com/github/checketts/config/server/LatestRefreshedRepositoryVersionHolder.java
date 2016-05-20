package com.github.checketts.config.server;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class LatestRefreshedRepositoryVersionHolder {

    public static final String UNKNOWN_REPOSITORY_VERSION = "unknown";

    private AtomicReference<String> latestRefreshedRepositoryVersion = new AtomicReference<>(UNKNOWN_REPOSITORY_VERSION);

    public String getLatestRefreshedRepositoryVersion() {
        return latestRefreshedRepositoryVersion.get();
    }

    public void setLatestRefreshedRepositoryVersion(String latestRefreshedRepositoryVersion) {
        this.latestRefreshedRepositoryVersion.set(latestRefreshedRepositoryVersion);
    }
}
