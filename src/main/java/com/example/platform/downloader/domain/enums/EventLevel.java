package com.example.platform.downloader.domain.enums;

/** Mức độ nghiêm trọng của từng dòng log hoặc event đã persist. */
public enum EventLevel {
    /** Thông tin tiến trình bình thường. */
    INFO,
    /** Cảnh báo nhưng chưa phải lỗi kết thúc job. */
    WARN,
    /** Dòng log lỗi. */
    ERROR
}
