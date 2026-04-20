package com.example.platform.downloader.domain.enums;

/** Vòng đời ở góc nhìn worker cho một job tải cụ thể. */
public enum JobState {
    /** Job vừa được materialize nhưng chưa vào hàng đợi thực thi. */
    ACCEPTED,
    /** Trạng thái trung gian hiếm dùng khi một job tự cần bước resolve bổ sung. */
    RESOLVING,
    /** Job đã sẵn sàng cho worker claim. */
    QUEUED,
    /** Worker đã claim và đang chạy tiến trình tải. */
    RUNNING,
    /** Đang hậu xử lý sau khi tải xong, ví dụ watermark, manifest hoặc đồng bộ file. */
    POST_PROCESSING,
    /** Tải và hậu xử lý hoàn tất. */
    COMPLETED,
    /** Thất bại và không retry nữa. */
    FAILED,
    /** Thất bại tạm thời, đang chờ mốc thời gian retry tiếp theo. */
    RETRY_WAIT,
    /** Không được phép hoặc không thể xử lý theo rule hiện tại. */
    BLOCKED
}
