package com.example.platform.downloader.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretCryptoServiceTest {

    @Test
    void encryptAndDecryptRoundTrip() {
        SecretCryptoService service = new SecretCryptoService("unit-test-master-key");

        var encrypted = service.encrypt("super-secret-value");

        assertThat(encrypted.payload()).isNotBlank();
        assertThat(encrypted.iv()).isNotBlank();
        assertThat(service.decrypt(encrypted.payload(), encrypted.iv())).isEqualTo("super-secret-value");
    }
}
