package com.example.platform.downloader.ui;

import com.example.platform.downloader.domain.RuntimeSettings;
import com.example.platform.downloader.application.SessionSettingsService;
import com.example.platform.kernel.exception.BusinessException;
import com.example.platform.kernel.ui.RestResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST API để quản lý RuntimeSettings trong session.
 *
 * Bảo mật:
 *   - Chỉ ADMIN hoặc PUBLISHER mới được gọi
 *   - GET không trả raw token — chỉ trả boolean hasXxx
 *   - DELETE xóa sạch settings khỏi session hiện tại
 *
 * Endpoints:
 *   POST   /downloader/api/runtime-settings  → lưu settings
 *   GET    /downloader/api/runtime-settings  → trả masked status
 *   DELETE /downloader/api/runtime-settings  → xóa settings
 */
@RestController
@RequestMapping("/downloader/api/runtime-settings")
@PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER')")
public class RuntimeSettingsController {

    private static final Logger log = LoggerFactory.getLogger(RuntimeSettingsController.class);

    private final SessionSettingsService sessionSettingsService;

    public RuntimeSettingsController(SessionSettingsService sessionSettingsService) {
        this.sessionSettingsService = sessionSettingsService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST — lưu settings vào session
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping
    public RestResponse<Void> saveSettings(@RequestBody RuntimeSettings incoming, HttpSession session) {
        try {
            sessionSettingsService.save(incoming, session);
            return RestResponse.ok(null, "✅ Settings đã được lưu vào session (tự xoá sau 30 phút).");
        } catch (IllegalArgumentException e) {
            log.warn("[RuntimeSettings] Validation failed: {}", e.getMessage());
            throw new BusinessException(e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET — trả về trạng thái masked (boolean only, không raw token)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    public RestResponse<Map<String, Object>> getSettingsStatus(HttpSession session) {
        Optional<RuntimeSettings> opt = sessionSettingsService.get(session);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasSettings", opt.isPresent());

        if (opt.isPresent()) {
            RuntimeSettings rs = opt.get();
            // Chỉ trả boolean — KHÔNG expose raw token/secret
            result.put("hasTelegramToken",           rs.hasTelegramBotToken());
            result.put("hasTelegramChatId",          rs.hasTelegramChatId());
            result.put("hasGoogleDriveServiceAccount", rs.hasGoogleDriveServiceAccountJson());
            result.put("hasGoogleDriveFolderId",      rs.hasGoogleDriveFolderId());
            result.put("hasBaseUrl",                  rs.hasBaseUrl());
        } else {
            result.put("hasTelegramToken",           false);
            result.put("hasTelegramChatId",          false);
            result.put("hasGoogleDriveServiceAccount", false);
            result.put("hasGoogleDriveFolderId",      false);
            result.put("hasBaseUrl",                  false);
        }

        return RestResponse.ok(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE — xóa settings khỏi session
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping
    public RestResponse<Void> clearSettings(HttpSession session) {
        sessionSettingsService.clear(session);
        return RestResponse.ok(null, "✅ Settings đã được xóa khỏi session.");
    }
}
