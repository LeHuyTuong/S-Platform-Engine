package com.example.platform.kernel.ui;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper for the platform.
 */
public class RestResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public RestResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public static <T> RestResponse<T> ok(T data, String message) {
        RestResponse<T> resp = new RestResponse<>();
        resp.setSuccess(true);
        resp.setData(data);
        resp.setMessage(message);
        return resp;
    }

    public static <T> RestResponse<T> ok(T data) {
        return ok(data, "Success");
    }

    public static <T> RestResponse<T> error(String message) {
        RestResponse<T> resp = new RestResponse<>();
        resp.setSuccess(false);
        resp.setMessage(message);
        return resp;
    }

    // Getters & Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
