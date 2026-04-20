package com.example.platform.downloader.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class SecretCryptoService {

    private static final String AES = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_SIZE = 12;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public SecretCryptoService(@Value("${app.security.master-key}") String masterKey) {
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                    .digest(masterKey.getBytes(StandardCharsets.UTF_8));
            this.keySpec = new SecretKeySpec(keyBytes, AES);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize crypto service", e);
        }
    }

    public EncryptedValue encrypt(String raw) {
        if (raw == null || raw.isBlank()) {
            return EncryptedValue.empty();
        }
        try {
            byte[] iv = new byte[IV_SIZE];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));
            return new EncryptedValue(
                    Base64.getEncoder().encodeToString(encrypted),
                    Base64.getEncoder().encodeToString(iv)
            );
        } catch (Exception e) {
            throw new IllegalStateException("Cannot encrypt secret", e);
        }
    }

    public String decrypt(String payload, String iv) {
        if (payload == null || payload.isBlank() || iv == null || iv.isBlank()) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(
                    GCM_TAG_BITS, Base64.getDecoder().decode(iv)
            ));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(payload));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot decrypt secret", e);
        }
    }

    public record EncryptedValue(String payload, String iv) {
        public static EncryptedValue empty() {
            return new EncryptedValue(null, null);
        }
    }
}
