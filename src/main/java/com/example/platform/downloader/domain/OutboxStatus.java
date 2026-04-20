package com.example.platform.downloader.domain;

/** Trạng thái phát hành/xử lý của một OutboxEvent. */
public enum OutboxStatus {
    /** Mới tạo, chưa publish. */
    PENDING,
    /** Đã publish ra local bus hoặc Redis stream. */
    PUBLISHED,
    /** Event đã được xử lý thành công. */
    PROCESSED,
    /** Publish/process lỗi, sẽ đợi retry hoặc cần replay. */
    FAILED
}
