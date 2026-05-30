package com.reviewbot.review.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reviewbot.repository.domain.RepositoryConfig;
import com.reviewbot.review.dto.AiReviewComment;
import com.reviewbot.review.dto.ClaudeResponse;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("AiReviewService — Claude API 연동 및 diff 처리")
class AiReviewServiceTest {

    private static MockWebServer mockServer;
    private AiReviewService aiReviewService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    static void startServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterAll
    static void stopServer() throws IOException {
        mockServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        WebClient claudeWebClient = WebClient.builder()
                .baseUrl(mockServer.url("/").toString())
                .build();

        aiReviewService = new AiReviewService(claudeWebClient, objectMapper);
        ReflectionTestUtils.setField(aiReviewService, "model",     "claude-test");
        ReflectionTestUtils.setField(aiReviewService, "maxTokens", 100);
    }

    @Test
    @DisplayName("Claude가 유효한 JSON 배열을 반환하면 리스트로 파싱")
    void analyzeCode_validResponse_returnsComments() throws Exception {
        // Given
        List<AiReviewComment> expected = List.of(
                new AiReviewComment("src/Foo.java", 10, "warning", "security", "SQL injection 위험")
        );
        enqueueClaudeResponse(expected);

        // When
        List<AiReviewComment> result = aiReviewService.analyzeCode(
                sampleDiff("src/Foo.java"), null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).filePath()).isEqualTo("src/Foo.java");
        assertThat(result.get(0).severity()).isEqualTo("warning");
        assertThat(result.get(0).category()).isEqualTo("security");
    }

    @Test
    @DisplayName("Claude가 마크다운 코드블록에 감싸도 파싱 성공")
    void analyzeCode_markdownWrapped_parsesCorrectly() throws Exception {
        // Given — 실제 Claude가 가끔 ```json ... ``` 으로 감싸는 경우
        List<AiReviewComment> expected = List.of(
                new AiReviewComment("Foo.java", null, "info", "style", "네이밍 개선 권장")
        );
        String json      = objectMapper.writeValueAsString(expected);
        String wrapped   = "```json\n" + json + "\n```";
        ClaudeResponse resp = new ClaudeResponse(
                List.of(new ClaudeResponse.ContentBlock("text", wrapped)), "end_turn");
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(resp))
                .addHeader("Content-Type", "application/json"));

        // When
        List<AiReviewComment> result = aiReviewService.analyzeCode(sampleDiff("Foo.java"), null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).severity()).isEqualTo("info");
    }

    @Test
    @DisplayName("diff가 비어있으면 Claude 호출 없이 빈 리스트 반환")
    void analyzeCode_emptyDiff_returnsEmptyWithoutApiCall() {
        List<AiReviewComment> result = aiReviewService.analyzeCode("", null);

        assertThat(result).isEmpty();
        assertThat(mockServer.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("ignorePatterns에 매칭되는 파일만 있으면 Claude 호출 스킵")
    void analyzeCode_allFilesIgnored_skipsApi() {
        RepositoryConfig config = mock(RepositoryConfig.class);
        when(config.getIgnorePatterns()).thenReturn(new String[]{"*.md"});
        when(config.getFocusAreas()).thenReturn(new String[]{"bug"});
        when(config.getReviewLanguage()).thenReturn("ko");

        int beforeCount = mockServer.getRequestCount();
        List<AiReviewComment> result = aiReviewService.analyzeCode(
                sampleDiff("README.md"), config);

        assertThat(result).isEmpty();
        assertThat(mockServer.getRequestCount()).isEqualTo(beforeCount); // 호출 없음
    }

    @Test
    @DisplayName("Claude 응답이 파싱 불가한 텍스트면 빈 리스트")
    void analyzeCode_unparsableResponse_returnsEmpty() throws Exception {
        ClaudeResponse resp = new ClaudeResponse(
                List.of(new ClaudeResponse.ContentBlock("text", "리뷰할 내용이 없습니다.")),
                "end_turn");
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(resp))
                .addHeader("Content-Type", "application/json"));

        List<AiReviewComment> result = aiReviewService.analyzeCode(sampleDiff("Foo.java"), null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("ignorePatterns와 매칭 안 되는 파일은 diff에 포함")
    void analyzeCode_nonIgnoredFile_includesInDiff() throws Exception {
        RepositoryConfig config = mock(RepositoryConfig.class);
        when(config.getIgnorePatterns()).thenReturn(new String[]{"*.md", "*.json"});
        when(config.getFocusAreas()).thenReturn(new String[]{"security"});
        when(config.getReviewLanguage()).thenReturn("ko");

        enqueueClaudeResponse(List.of(
                new AiReviewComment("src/Main.java", 5, "critical", "security", "문제 발견")));

        List<AiReviewComment> result = aiReviewService.analyzeCode(
                sampleDiff("src/Main.java"), config);

        assertThat(result).hasSize(1);
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private void enqueueClaudeResponse(List<AiReviewComment> comments) throws Exception {
        String json = objectMapper.writeValueAsString(comments);
        ClaudeResponse resp = new ClaudeResponse(
                List.of(new ClaudeResponse.ContentBlock("text", json)), "end_turn");
        mockServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(resp))
                .addHeader("Content-Type", "application/json"));
    }

    private String sampleDiff(String filePath) {
        return """
                diff --git a/%s b/%s
                index abc123..def456 100644
                --- a/%s
                +++ b/%s
                @@ -1,3 +1,4 @@
                 public class Foo {
                +    String query = "SELECT * FROM users WHERE id=" + id;
                 }
                """.formatted(filePath, filePath, filePath, filePath);
    }
}
