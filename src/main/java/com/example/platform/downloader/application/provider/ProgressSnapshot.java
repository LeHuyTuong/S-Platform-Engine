package com.example.platform.downloader.application.provider;

public class ProgressSnapshot {

    private Double progressPercent;
    private String downloadSpeed;
    private String eta;
    private Integer currentItem;
    private Integer totalItems;
    private String playlistTitle;
    private String detectedTitle;

    public Double getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(Double progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getDownloadSpeed() {
        return downloadSpeed;
    }

    public void setDownloadSpeed(String downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public String getEta() {
        return eta;
    }

    public void setEta(String eta) {
        this.eta = eta;
    }

    public Integer getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(Integer currentItem) {
        this.currentItem = currentItem;
    }

    public Integer getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(Integer totalItems) {
        this.totalItems = totalItems;
    }

    public String getPlaylistTitle() {
        return playlistTitle;
    }

    public void setPlaylistTitle(String playlistTitle) {
        this.playlistTitle = playlistTitle;
    }

    public String getDetectedTitle() {
        return detectedTitle;
    }

    public void setDetectedTitle(String detectedTitle) {
        this.detectedTitle = detectedTitle;
    }
}
