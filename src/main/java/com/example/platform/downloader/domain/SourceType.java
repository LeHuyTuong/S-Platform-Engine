package com.example.platform.downloader.domain;

/** Kiểu nguồn đầu vào trước khi resolve thành Job cụ thể. */
public enum SourceType {
    /** Một URL trỏ trực tiếp tới một item media cụ thể. */
    DIRECT_URL,
    /** Một URL chứa danh sách nhiều item và cần fan-out thành nhiều Job. */
    PLAYLIST,
    /** Một URL hồ sơ/kênh/tài khoản, trước tiên phải resolve danh sách item. */
    PROFILE
}
