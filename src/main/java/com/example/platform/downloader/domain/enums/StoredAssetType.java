package com.example.platform.downloader.domain.enums;

/** Nhóm loại file tổng quát được ghi vào `stored_assets` và `manifest.json`. */
public enum StoredAssetType {
    /** File media chính như video hoặc audio. */
    MEDIA,
    /** File thumbnail ảnh. */
    THUMBNAIL,
    /** File phụ đề. */
    SUBTITLE,
    /** File mô tả hoặc text đi kèm. */
    DESCRIPTION,
    /** `manifest.json` tổng hợp metadata và file output. */
    MANIFEST,
    /** File khác chưa phân loại rõ. */
    OTHER
}
