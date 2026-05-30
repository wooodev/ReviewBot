package com.reviewbot.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider — 토큰 생성/검증")
class JwtTokenProviderTest {

    private static final String USER_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String SECRET  = "test-jwt-secret-must-be-at-least-32-characters-here!";

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret",             SECRET);
        ReflectionTestUtils.setField(provider, "accessTokenExpiry",  3_600_000L);
        ReflectionTestUtils.setField(provider, "refreshTokenExpiry", 604_800_000L);
        provider.init();
    }

    @Test
    @DisplayName("액세스 토큰 생성 → 유효성 통과, userId 추출")
    void accessToken_createAndValidate() {
        String token = provider.createAccessToken(USER_ID);

        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.getUserId(token)).isEqualTo(USER_ID);
        assertThat(provider.getTokenType(token)).isEqualTo("access");
    }

    @Test
    @DisplayName("리프레시 토큰 생성 → type = refresh")
    void refreshToken_type() {
        String token = provider.createRefreshToken(USER_ID);

        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.getTokenType(token)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("잘못된 토큰은 isValid() = false")
    void invalidToken_isNotValid() {
        assertThat(provider.isValid("not.a.token")).isFalse();
        assertThat(provider.isValid("")).isFalse();
        assertThat(provider.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("시크릿이 32자 미만이면 초기화 실패")
    void init_shortSecret_throws() {
        JwtTokenProvider p = new JwtTokenProvider();
        ReflectionTestUtils.setField(p, "secret",             "short");
        ReflectionTestUtils.setField(p, "accessTokenExpiry",  3_600_000L);
        ReflectionTestUtils.setField(p, "refreshTokenExpiry", 604_800_000L);

        assertThatThrownBy(p::init).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("만료된 토큰은 isValid() = false")
    void expiredToken_isNotValid() {
        JwtTokenProvider shortLived = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortLived, "secret",             SECRET);
        ReflectionTestUtils.setField(shortLived, "accessTokenExpiry",  1L);   // 1ms
        ReflectionTestUtils.setField(shortLived, "refreshTokenExpiry", 604_800_000L);
        shortLived.init();

        String token = shortLived.createAccessToken(USER_ID);
        // 토큰이 즉시 만료되도록 1ms 후 검증
        assertThat(shortLived.isValid(token)).isFalse();
    }
}
