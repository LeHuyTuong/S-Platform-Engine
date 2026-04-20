package com.example.platform.downloader.application.provider;

import com.example.platform.downloader.domain.enums.SourceType;

import java.util.ArrayList;
import java.util.List;

public class SourceResolution {

    private SourceType resolvedSourceType;
    private final List<ResolvedItem> items = new ArrayList<>();
    private boolean blocked;
    private String blockedReason;

    public SourceType getResolvedSourceType() {
        return resolvedSourceType;
    }

    public void setResolvedSourceType(SourceType resolvedSourceType) {
        this.resolvedSourceType = resolvedSourceType;
    }

    public List<ResolvedItem> getItems() {
        return items;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public String getBlockedReason() {
        return blockedReason;
    }

    public void setBlockedReason(String blockedReason) {
        this.blockedReason = blockedReason;
    }
}
