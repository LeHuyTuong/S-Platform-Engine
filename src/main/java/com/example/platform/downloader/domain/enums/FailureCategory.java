package com.example.platform.downloader.domain.enums;

/** Nhóm lỗi chuẩn hóa để quyết định retry và thông điệp hiển thị. */
public enum FailureCategory {
    /** Không có lỗi hoặc chưa phân loại lỗi. */
    NONE,
    /** Bị rate limit, thường có thể retry. */
    RATE_LIMIT,
    /** Lỗi tạm thời từ mạng, hạ tầng hoặc provider; thường có thể retry. */
    TEMPORARY,
    /** URL đầu vào sai hoặc không parse được. */
    INVALID_URL,
    /** Nội dung private, cần quyền mà hệ thống hiện không có. */
    PRIVATE_CONTENT,
    /** Nội dung đã bị xóa hoặc không còn tồn tại. */
    REMOVED,
    /** Bị từ chối quyền truy cập. */
    PERMISSION_DENIED,
    /** Provider hoặc loại URL chưa hỗ trợ. */
    UNSUPPORTED,
    /** Lỗi process nội bộ khi chạy command. */
    PROCESS_ERROR,
    /** Tiến trình bị kill do quá thời gian timeout. */
    TIMEOUT,
    /** Lỗi chưa phân loại được. */
    UNKNOWN
}
