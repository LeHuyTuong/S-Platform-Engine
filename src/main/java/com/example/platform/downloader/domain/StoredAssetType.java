package com.example.platform.downloader.domain;

/** Nhóm loại file tổng quát được ghi vào stored_assets và manifest.json. */
public enum StoredAssetType {
    /** File media chính như video/audio. */
    MEDIA,
    /** File thumbnail ảnh. */
    THUMBNAIL,
    /** File phụ đề. */
    SUBTITLE,
    /** File mô tả/text đi kèm. */
    DESCRIPTION,
    /** manifest.json tổng hợp metadata/file output. */
    MANIFEST,
    /** File khác chưa phân loại rõ. */
    OTHER
}
