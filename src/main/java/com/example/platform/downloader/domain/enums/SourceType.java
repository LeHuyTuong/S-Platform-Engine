package com.example.platform.downloader.domain.enums;

/** Kiểu nguồn đầu vào trước khi resolve thành job cụ thể. */
public enum SourceType {
    /** Một URL trỏ trực tiếp tới một item media cụ thể. */
    DIRECT_URL,
    /** Một URL chứa danh sách nhiều item và cần fan-out thành nhiều job. */
    PLAYLIST,
    /** Một URL hồ sơ, kênh hoặc tài khoản; trước tiên phải resolve danh sách item. */
    PROFILE
}
