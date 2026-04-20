package com.example.platform.downloader.domain;

/** Vòng đời của SourceRequest từ lúc API nhận vào đến khi resolve xong. */
public enum SourceRequestState {
    /** API vừa nhận request và đã persist xong. */
    ACCEPTED,
    /** Worker đang resolve URL thành item cụ thể. */
    RESOLVING,
    /** Resolve xong, đã có số lượng item/job tương ứng. */
    RESOLVED,
    /** Request bị chặn do vượt policy hoặc provider trả về trạng thái không cho xử lý. */
    BLOCKED,
    /** Resolve thất bại ngoài dự kiến. */
    FAILED
}
