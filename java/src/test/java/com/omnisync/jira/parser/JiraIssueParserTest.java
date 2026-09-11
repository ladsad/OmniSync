package com.omnisync.jira.parser;

import com.omnisync.core.error.MalformedDataException;
import com.omnisync.jira.model.JiraIssue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JiraIssueParserTest {

    private final JiraIssueParser parser = new JiraIssueParser();

    @Test
    @DisplayName("parseSearchResponse successfully parses issues and pagination metadata")
    void parsesValidResponse() {
        String json = """
                {
                  "startAt": 0,
                  "maxResults": 50,
                  "total": 1,
                  "issues": [
                    {
                      "id": "1001",
                      "key": "PROJ-10",
                      "fields": {
                        "summary": "Fix authentication crash",
                        "status": { "name": "Done" },
                        "issuetype": { "name": "Bug" },
                        "priority": { "name": "High" },
                        "created": "2026-09-01T10:00:00.000Z",
                        "updated": "2026-09-02T12:00:00.000Z",
                        "assignee": { "displayName": "Bob Doe" }
                      }
                    }
                  ]
                }
                """;

        JiraSearchResult result = parser.parseSearchResponse(json);

        assertThat(result.startAt()).isEqualTo(0);
        assertThat(result.maxResults()).isEqualTo(50);
        assertThat(result.total()).isEqualTo(1);
        assertThat(result.issues()).hasSize(1);

        JiraIssue issue = result.issues().get(0);
        assertThat(issue.id()).isEqualTo("1001");
        assertThat(issue.key()).isEqualTo("PROJ-10");
        assertThat(issue.summary()).isEqualTo("Fix authentication crash");
        assertThat(issue.status()).isEqualTo("Done");
        assertThat(issue.issueType()).isEqualTo("Bug");
        assertThat(issue.priority()).isEqualTo("High");
        assertThat(issue.created()).isEqualTo("2026-09-01T10:00:00.000Z");
        assertThat(issue.updated()).isEqualTo("2026-09-02T12:00:00.000Z");
        assertThat(issue.assignee()).isEqualTo("Bob Doe");
    }

    @Test
    @DisplayName("parseSearchResponse defensively handles missing optional fields and skips corrupt items")
    void handlesMissingFieldsAndSkipsCorrupt() {
        String json = """
                {
                  "startAt": 10,
                  "maxResults": 20,
                  "total": 30,
                  "issues": [
                    {
                      "id": "1002",
                      "key": "PROJ-20",
                      "fields": {}
                    },
                    {
                      "corrupt": "missing id and key"
                    },
                    null
                  ]
                }
                """;

        JiraSearchResult result = parser.parseSearchResponse(json);
        assertThat(result.issues()).hasSize(1);

        JiraIssue issue = result.issues().get(0);
        assertThat(issue.id()).isEqualTo("1002");
        assertThat(issue.key()).isEqualTo("PROJ-20");
        assertThat(issue.summary()).isEmpty();
        assertThat(issue.status()).isEqualTo("Unknown");
        assertThat(issue.assignee()).isEqualTo("Unassigned");
    }

    @Test
    @DisplayName("parseSearchResponse throws MalformedDataException on invalid json")
    void throwsOnInvalidJson() {
        assertThatThrownBy(() -> parser.parseSearchResponse("{invalid-json"))
                .isInstanceOf(MalformedDataException.class);

        assertThatThrownBy(() -> parser.parseSearchResponse("[]"))
                .isInstanceOf(MalformedDataException.class)
                .hasMessageContaining("root must be a JSON object");

        assertThatThrownBy(() -> parser.parseSearchResponse(""))
                .isInstanceOf(MalformedDataException.class);
    }
}
