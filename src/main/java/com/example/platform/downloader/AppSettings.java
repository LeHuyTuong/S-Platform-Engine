package com.example.platform.downloader;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cấu hình toàn hệ thống - thay đổi nóng không cần restart Docker.
 * Admin có thể chỉnh từ giao diện /admin khi YouTube bắt đầu chặn IP.
 */
@Component
public class AppSettings {

    // Số luồng tải song song (1 = chậm nhưng an toàn nhất)
    private final AtomicInteger concurrentFragments = new AtomicInteger(1);

    // Thời gian nghỉ giữa các video trong playlist (giây)
    private final AtomicInteger sleepInterval = new AtomicInteger(15);

    // Thời gian nghỉ giữa các API request nội bộ (giây)
    private final AtomicInteger sleepRequests = new AtomicInteger(2);

    // Số lần thử lại khi lỗi mạng
    private final AtomicInteger retries = new AtomicInteger(10);

    // Dung lượng tối đa cho phép tải (MB, 0 = vô hạn)
    private final AtomicLong maxFileSizeMb = new AtomicLong(0);

    // Getters
    public int getConcurrentFragments() { return concurrentFragments.get(); }
    public int getSleepInterval() { return sleepInterval.get(); }
    public int getSleepRequests() { return sleepRequests.get(); }
    public int getRetries() { return retries.get(); }
    public long getMaxFileSizeMb() { return maxFileSizeMb.get(); }

    // Setters (thread-safe, có thể gọi từ bất kỳ thread nào)
    public void setConcurrentFragments(int v) { concurrentFragments.set(v); }
    public void setSleepInterval(int v) { sleepInterval.set(v); }
    public void setSleepRequests(int v) { sleepRequests.set(v); }
    public void setRetries(int v) { retries.set(v); }
    public void setMaxFileSizeMb(long v) { maxFileSizeMb.set(v); }
}
