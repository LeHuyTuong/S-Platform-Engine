package com.example.platform.modules.user.ui.dto;

public class AuthSessionResponse {

    private boolean authenticated;
    private String email;
    private String role;
    private boolean enabled;
    private int dailyQuota;
    private int jobsToday;
    private boolean canUseProxy;
    private boolean canManageRuntimeSettings;

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getDailyQuota() {
        return dailyQuota;
    }

    public void setDailyQuota(int dailyQuota) {
        this.dailyQuota = dailyQuota;
    }

    public int getJobsToday() {
        return jobsToday;
    }

    public void setJobsToday(int jobsToday) {
        this.jobsToday = jobsToday;
    }

    public boolean isCanUseProxy() {
        return canUseProxy;
    }

    public void setCanUseProxy(boolean canUseProxy) {
        this.canUseProxy = canUseProxy;
    }

    public boolean isCanManageRuntimeSettings() {
        return canManageRuntimeSettings;
    }

    public void setCanManageRuntimeSettings(boolean canManageRuntimeSettings) {
        this.canManageRuntimeSettings = canManageRuntimeSettings;
    }
}
