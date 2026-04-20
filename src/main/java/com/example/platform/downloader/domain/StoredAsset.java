package com.example.platform.downloader.domain;

import com.example.platform.kernel.domain.BaseAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "stored_assets")
/**
 * Danh mục file được tạo ra từ một Job.
 *
 * Sau khi tải xong và hậu xử lý xong, worker sẽ quét lại thư mục job rồi đồng bộ
 * các file vào bảng này để UI/API liệt kê file an toàn mà không lộ path tùy ý.
 */
public class StoredAsset extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Job cha sở hữu file này.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    // Phân loại nhẹ để UI và manifest biết đây là file gì.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private StoredAssetType assetType;

    // Chỉ lưu relative path để storage dễ di chuyển giữa môi trường.
    @Column(nullable = false, length = 500)
    private String fileName;

    @Column(nullable = false, length = 1000)
    private String relativePath;

    @Column(length = 255)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    // Checksum dự phòng cho bước verify/integrity trong tương lai.
    @Column(length = 128)
    private String checksumSha256;

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public StoredAssetType getAssetType() {
        return assetType;
    }

    public void setAssetType(StoredAssetType assetType) {
        this.assetType = assetType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getChecksumSha256() {
        return checksumSha256;
    }

    public void setChecksumSha256(String checksumSha256) {
        this.checksumSha256 = checksumSha256;
    }
}
