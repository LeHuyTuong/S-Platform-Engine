package com.example.platform.downloader.ui.dto;

public class AdminJobSummaryResponse extends JobStatusResponse {

    private String ownerEmail;
    private String ownerRole;

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public String getOwnerRole() {
        return ownerRole;
    }

    public void setOwnerRole(String ownerRole) {
        this.ownerRole = ownerRole;
    }
}
