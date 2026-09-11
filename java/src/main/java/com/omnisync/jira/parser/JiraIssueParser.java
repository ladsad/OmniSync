package com.omnisync.jira.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisync.core.error.MalformedDataException;
import com.omnisync.jira.model.JiraIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * Defensive parser for transforming Jira REST search responses into typed domain models.
 */
public class JiraIssueParser {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a JiraIssueParser using a default Jackson ObjectMapper.
     */
    public JiraIssueParser() {
        this(new ObjectMapper());
    }

    /**
     * Constructs a JiraIssueParser with a custom ObjectMapper.
     *
     * @param objectMapper configured Jackson object mapper
     */
    public JiraIssueParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Parses a raw JSON response string from Jira search endpoints into a JiraSearchResult.
     *
     * @param rawJson the response payload from Jira API
     * @return parsed search result containing pagination metadata and valid issues
     * @throws MalformedDataException if JSON parsing fails entirely or root structure is invalid
     */
    public JiraSearchResult parseSearchResponse(String rawJson) throws MalformedDataException {
        if (rawJson == null || rawJson.isBlank()) {
            throw new MalformedDataException("Empty or null Jira JSON payload", rawJson);
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isObject()) {
                throw new MalformedDataException("Jira response root must be a JSON object", rawJson);
            }

            int startAt = root.path("startAt").asInt(0);
            int maxResults = root.path("maxResults").asInt(50);
            int total = root.path("total").asInt(0);

            List<JiraIssue> issues = new ArrayList<>();
            JsonNode issuesNode = root.path("issues");
            if (issuesNode.isArray()) {
                for (JsonNode node : issuesNode) {
                    JiraIssue issue = parseSingleIssue(node);
                    if (issue != null) {
                        issues.add(issue);
                    }
                }
            }

            return new JiraSearchResult(startAt, maxResults, total, issues);
        } catch (MalformedDataException e) {
            throw e;
        } catch (Exception e) {
            throw new MalformedDataException("Failed to parse Jira response: " + e.getMessage(), rawJson, e);
        }
    }

    /**
     * Parses an individual issue JsonNode defensively, skipping if mandatory identifiers are missing.
     *
     * @param node issue JsonNode
     * @return JiraIssue instance or null if malformed
     */
    protected JiraIssue parseSingleIssue(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }

        String id = node.path("id").asText(null);
        String key = node.path("key").asText(null);
        if (id == null || key == null || id.isBlank() || key.isBlank()) {
            // Defensive skipping of unidentifiable issue record
            return null;
        }

        JsonNode fields = node.path("fields");
        String summary = fields.path("summary").asText("");
        String status = fields.path("status").path("name").asText("Unknown");
        String issueType = fields.path("issuetype").path("name").asText("Unknown");
        String priority = fields.path("priority").path("name").asText("None");
        String created = fields.path("created").asText("");
        String updated = fields.path("updated").asText("");

        JsonNode assigneeNode = fields.path("assignee");
        String assignee = assigneeNode.isObject()
                ? assigneeNode.path("displayName").asText("Unassigned")
                : "Unassigned";

        return new JiraIssue(id, key, summary, status, issueType, priority, created, updated, assignee);
    }
}
