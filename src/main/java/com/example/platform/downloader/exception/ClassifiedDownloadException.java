package com.example.platform.downloader.exception;

import com.example.platform.downloader.domain.enums.FailureCategory;

public class ClassifiedDownloadException extends RuntimeException {

    private final FailureCategory failureCategory;

    public ClassifiedDownloadException(FailureCategory failureCategory, String message) {
        super(message);
        this.failureCategory = failureCategory;
    }

    public FailureCategory getFailureCategory() {
        return failureCategory;
    }
}

