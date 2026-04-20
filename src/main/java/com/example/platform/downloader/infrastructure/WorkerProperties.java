package com.example.platform.downloader.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.worker")
public class WorkerProperties {

    private boolean enabled = true;
    private boolean redisEnabled;
    private String workerId = "local-worker";
    private String streamKey = "download-jobs";
    private String consumerGroup = "platform-workers";
    private int concurrency = 4;
    private Map<String, Integer> providerLimits = new HashMap<>();
    private int resolveTimeoutSeconds = 30;
    private int downloadTimeoutMinutes = 30;
    private int postProcessTimeoutMinutes = 10;
    private List<Integer> retryBackoffSeconds = List.of(60, 300, 900);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public Map<String, Integer> getProviderLimits() {
        return providerLimits;
    }

    public void setProviderLimits(Map<String, Integer> providerLimits) {
        this.providerLimits = providerLimits;
    }

    public int getResolveTimeoutSeconds() {
        return resolveTimeoutSeconds;
    }

    public void setResolveTimeoutSeconds(int resolveTimeoutSeconds) {
        this.resolveTimeoutSeconds = resolveTimeoutSeconds;
    }

    public int getDownloadTimeoutMinutes() {
        return downloadTimeoutMinutes;
    }

    public void setDownloadTimeoutMinutes(int downloadTimeoutMinutes) {
        this.downloadTimeoutMinutes = downloadTimeoutMinutes;
    }

    public int getPostProcessTimeoutMinutes() {
        return postProcessTimeoutMinutes;
    }

    public void setPostProcessTimeoutMinutes(int postProcessTimeoutMinutes) {
        this.postProcessTimeoutMinutes = postProcessTimeoutMinutes;
    }

    public List<Integer> getRetryBackoffSeconds() {
        return retryBackoffSeconds;
    }

    public void setRetryBackoffSeconds(List<Integer> retryBackoffSeconds) {
        this.retryBackoffSeconds = retryBackoffSeconds;
    }
}
