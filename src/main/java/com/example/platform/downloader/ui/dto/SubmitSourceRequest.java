package com.example.platform.downloader.ui.dto;

public class SubmitSourceRequest {

    private String sourceUrl;
    private String url;
    private String platform;
    private String sourceType;
    private String downloadType;
    private String quality;
    private String format;
    private boolean writeThumbnail;
    private boolean cleanMetadata;
    private String startTime;
    private String endTime;
    private String proxyRef;
    private String proxy;
    private String titleTemplate;
    private String watermarkText;

    public String effectiveSourceUrl() {
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            return sourceUrl.strip();
        }
        return url == null ? null : url.strip();
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getDownloadType() {
        return downloadType;
    }

    public void setDownloadType(String downloadType) {
        this.downloadType = downloadType;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isWriteThumbnail() {
        return writeThumbnail;
    }

    public void setWriteThumbnail(boolean writeThumbnail) {
        this.writeThumbnail = writeThumbnail;
    }

    public boolean isCleanMetadata() {
        return cleanMetadata;
    }

    public void setCleanMetadata(boolean cleanMetadata) {
        this.cleanMetadata = cleanMetadata;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getProxyRef() {
        return proxyRef;
    }

    public void setProxyRef(String proxyRef) {
        this.proxyRef = proxyRef;
    }

    public String getProxy() {
        return proxy;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public String getTitleTemplate() {
        return titleTemplate;
    }

    public void setTitleTemplate(String titleTemplate) {
        this.titleTemplate = titleTemplate;
    }

    public String getWatermarkText() {
        return watermarkText;
    }

    public void setWatermarkText(String watermarkText) {
        this.watermarkText = watermarkText;
    }
}
