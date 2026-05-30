package com.reviewbot.github.service;

import com.reviewbot.common.exception.InvalidWebhookSignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("WebhookVerifier — HMAC-SHA256 서명 검증")
class WebhookVerifierTest {

    private static final String SECRET  = "test-webhook-secret";
    private static final String PAYLOAD = "{\"action\":\"opened\"}";

    private WebhookVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new WebhookVerifier();
        ReflectionTestUtils.setField(verifier, "webhookSecret", SECRET);
    }

    @Test
    @DisplayName("올바른 서명이면 예외 없음")
    void verify_validSignature_success() throws Exception {
        String signature = "sha256=" + hmac(PAYLOAD, SECRET);
        assertThatCode(() -> verifier.verify(PAYLOAD, signature)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("서명이 다르면 InvalidWebhookSignatureException")
    void verify_tamperedSignature_throws() {
        assertThatThrownBy(() -> verifier.verify(PAYLOAD, "sha256=badhash"))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    @DisplayName("서명이 null이면 예외")
    void verify_nullSignature_throws() {
        assertThatThrownBy(() -> verifier.verify(PAYLOAD, null))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    @DisplayName("sha256= 접두사 없으면 예외")
    void verify_missingPrefix_throws() {
        assertThatThrownBy(() -> verifier.verify(PAYLOAD, "invalidsignature"))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    @Test
    @DisplayName("페이로드가 변조되면 검증 실패")
    void verify_tamperedPayload_throws() throws Exception {
        String signature = "sha256=" + hmac(PAYLOAD, SECRET);
        assertThatThrownBy(() -> verifier.verify("{\"action\":\"closed\"}", signature))
                .isInstanceOf(InvalidWebhookSignatureException.class);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private String hmac(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(hash.length * 2);
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
