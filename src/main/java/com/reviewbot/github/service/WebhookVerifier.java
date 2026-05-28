package com.reviewbot.github.service;

import com.reviewbot.common.exception.InvalidWebhookSignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class WebhookVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    public void verify(String payload, String signature) {
        if (signature == null || !signature.startsWith("sha256=")) {
            throw new InvalidWebhookSignatureException("Missing or malformed webhook signature");
        }
        String expected = "sha256=" + hmacSha256(payload);
        if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                                   signature.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidWebhookSignatureException("Webhook signature mismatch");
        }
    }

    private String hmacSha256(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
