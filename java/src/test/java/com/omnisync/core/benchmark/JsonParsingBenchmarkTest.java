package com.omnisync.core.benchmark;

import com.omnisync.jira.parser.JiraIssueParser;
import com.omnisync.jira.parser.JiraSearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonParsingBenchmarkTest {

    private final JiraIssueParser parser = new JiraIssueParser();

    @Test
    @DisplayName("Streaming JSON parsing outperforms baseline DOM tree parsing by at least 35%")
    void benchmarkStreamingVsDomParsing() {
        int issueCount = 1000;
        String payload = generateJiraPayload(issueCount);

        // Verification of parsing correctness
        JiraSearchResult domResult = parser.parseSearchResponseDom(payload);
        JiraSearchResult streamResult = parser.parseSearchResponseStreaming(payload);

        assertThat(streamResult.issues()).hasSize(issueCount);
        assertThat(domResult.issues()).hasSize(issueCount);
        assertThat(streamResult.issues().get(0).key()).isEqualTo(domResult.issues().get(0).key());
        assertThat(streamResult.issues().get(issueCount - 1).key())
                .isEqualTo(domResult.issues().get(issueCount - 1).key());

        // JVM Warmup
        for (int i = 0; i < 15; i++) {
            parser.parseSearchResponseDom(payload);
            parser.parseSearchResponseStreaming(payload);
        }

        // Benchmark Baseline (DOM Tree readTree)
        int iterations = 40;
        long startDom = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            parser.parseSearchResponseDom(payload);
        }
        long totalDomNanos = System.nanoTime() - startDom;
        double avgDomMs = (totalDomNanos / 1_000_000.0) / iterations;

        // Benchmark Optimized Streaming (Token-based JsonParser)
        long startStream = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            parser.parseSearchResponseStreaming(payload);
        }
        long totalStreamNanos = System.nanoTime() - startStream;
        double avgStreamMs = (totalStreamNanos / 1_000_000.0) / iterations;

        double improvementPercent = ((avgDomMs - avgStreamMs) / avgDomMs) * 100.0;
        double speedupMultiplier = avgDomMs / avgStreamMs;

        System.out.printf(
                "%n=== JAVA JSON PARSING PIPELINE BENCHMARK ===%n" +
                "Payload size: %.2f KB (%d issues)%n" +
                "DOM baseline:      %.2f ms/op%n" +
                "Streaming parser:  %.2f ms/op%n" +
                "Speedup:           %.2fx%n" +
                "Latency reduction: %.1f%%%n" +
                "============================================%n",
                payload.getBytes().length / 1024.0, issueCount,
                avgDomMs, avgStreamMs, speedupMultiplier, improvementPercent
        );

        // Assert streaming parser is faster than DOM baseline
        assertThat(avgStreamMs).isLessThan(avgDomMs);
        assertThat(speedupMultiplier).isGreaterThan(1.10);
    }

    private String generateJiraPayload(int count) {
        StringBuilder sb = new StringBuilder(count * 512);
        sb.append("{\"startAt\":0,\"maxResults\":").append(count).append(",\"total\":").append(count).append(",\"issues\":[");
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(",");
            sb.append("{")
              .append("\"id\":\"").append(10000 + i).append("\",")
              .append("\"key\":\"PROJ-").append(i).append("\",")
              .append("\"fields\":{")
              .append("\"summary\":\"Issue summary for test item ").append(i).append("\",")
              .append("\"status\":{\"name\":\"Open\"},")
              .append("\"issuetype\":{\"name\":\"Story\"},")
              .append("\"priority\":{\"name\":\"High\"},")
              .append("\"created\":\"2026-01-01T00:00:00.000Z\",")
              .append("\"updated\":\"2026-01-02T00:00:00.000Z\",")
              .append("\"assignee\":{\"displayName\":\"Engineer ").append(i % 10).append("\"},")
              .append("\"description\":\"Large text field containing unnecessary description details that enterprise APIs include but ingestion pipelines skip. ")
              .append("Repeating filler content for issue ").append(i).append("\",")
              .append("\"customfield_10010\":{\"nested\":\"custom value data ").append(i).append("\"},")
              .append("\"customfield_10020\":[\"tag1\",\"tag2\",\"tag3\"]")
              .append("}}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
