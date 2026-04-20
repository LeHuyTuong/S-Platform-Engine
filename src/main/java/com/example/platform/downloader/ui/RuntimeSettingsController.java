package com.example.platform.downloader.ui;

import com.example.platform.downloader.application.UserConnectionSettingsService;
import com.example.platform.downloader.domain.RuntimeSettings;
import com.example.platform.kernel.exception.BusinessException;
import com.example.platform.kernel.exception.ResourceNotFoundException;
import com.example.platform.kernel.ui.RestResponse;
import com.example.platform.modules.user.domain.User;
import com.example.platform.modules.user.infrastructure.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/downloader/api/runtime-settings")
@PreAuthorize("hasAnyRole('ADMIN', 'PUBLISHER')")
public class RuntimeSettingsController {

    private final UserConnectionSettingsService userConnectionSettingsService;
    private final UserRepository userRepository;

    public RuntimeSettingsController(UserConnectionSettingsService userConnectionSettingsService,
                                     UserRepository userRepository) {
        this.userConnectionSettingsService = userConnectionSettingsService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public RestResponse<Void> saveSettings(@RequestBody RuntimeSettings incoming, Principal principal) {
        User user = currentUser(principal);
        try {
            userConnectionSettingsService.save(incoming, user);
            return RestResponse.ok(null, "Settings đã được lưu an toàn.");
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @GetMapping
    public RestResponse<Map<String, Object>> getSettingsStatus(Principal principal) {
        User user = currentUser(principal);
        RuntimeSettings masked = userConnectionSettingsService.getMasked(user);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("hasSettings", masked.isValid());
        result.put("hasTelegramToken", masked.hasTelegramBotToken());
        result.put("hasTelegramChatId", masked.hasTelegramChatId());
        result.put("hasGoogleDriveServiceAccount", masked.hasGoogleDriveServiceAccountJson());
        result.put("hasGoogleDriveFolderId", masked.hasGoogleDriveFolderId());
        result.put("hasBaseUrl", masked.hasBaseUrl());
        return RestResponse.ok(result);
    }

    @DeleteMapping
    public RestResponse<Void> clearSettings(Principal principal) {
        userConnectionSettingsService.clear(currentUser(principal));
        return RestResponse.ok(null, "Settings đã được xóa.");
    }

    private User currentUser(Principal principal) {
        if (principal == null) {
            throw new BusinessException("Bạn chưa đăng nhập");
        }
        return userRepository.findByEmail(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
