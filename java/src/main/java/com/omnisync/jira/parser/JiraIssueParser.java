package com.omnisync.jira.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisync.core.error.MalformedDataException;
import com.omnisync.core.json.JsonParsingPipeline;
import com.omnisync.jira.model.JiraIssue;

import java.util.ArrayList;
import java.util.List;

/**
 * Defensive parser for transforming Jira REST search responses into typed domain models.
 */
public class JiraIssueParser {

    private final ObjectMapper objectMapper;

    private final JsonParsingPipeline pipeline;

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
        this.pipeline = new JsonParsingPipeline(this.objectMapper.getFactory());
    }

    /**
     * Parses a raw JSON response string from Jira search endpoints using high-throughput streaming.
     *
     * @param rawJson the response payload from Jira API
     * @return parsed search result containing pagination metadata and valid issues
     * @throws MalformedDataException if JSON parsing fails entirely or root structure is invalid
     */
    public JiraSearchResult parseSearchResponse(String rawJson) throws MalformedDataException {
        return parseSearchResponseStreaming(rawJson);
    }

    /**
     * High-performance streaming parser reading token-by-token without building full DOM trees.
     *
     * @param rawJson the response payload from Jira API
     * @return parsed search result containing pagination metadata and valid issues
     * @throws MalformedDataException if JSON parsing fails entirely or root structure is invalid
     */
    public JiraSearchResult parseSearchResponseStreaming(String rawJson) throws MalformedDataException {
        return pipeline.parseString(rawJson, parser -> {
            if (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                throw new MalformedDataException("Jira response root must be a JSON object", rawJson);
            }

            int startAt = 0;
            int maxResults = 50;
            int total = 0;
            List<JiraIssue> issues = new ArrayList<>();

            while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
                String fieldName = parser.currentName();
                if ("startAt".equals(fieldName)) {
                    parser.nextToken();
                    startAt = parser.getValueAsInt(0);
                } else if ("maxResults".equals(fieldName)) {
                    parser.nextToken();
                    maxResults = parser.getValueAsInt(50);
                } else if ("total".equals(fieldName)) {
                    parser.nextToken();
                    total = parser.getValueAsInt(0);
                } else if ("issues".equals(fieldName)) {
                    if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_ARRAY) {
                        while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_ARRAY && parser.currentToken() != null) {
                            if (parser.currentToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                                JiraIssue issue = parseSingleIssueStreaming(parser);
                                if (issue != null) {
                                    issues.add(issue);
                                }
                            } else {
                                parser.skipChildren();
                            }
                        }
                    }
                } else {
                    parser.nextToken();
                    parser.skipChildren();
                }
            }

            return new JiraSearchResult(startAt, maxResults, total, issues);
        });
    }

    /**
     * Baseline DOM-based parser utilizing ObjectMapper readTree.
     *
     * @param rawJson raw JSON response
     * @return parsed search result
     * @throws MalformedDataException if JSON parsing fails
     */
    public JiraSearchResult parseSearchResponseDom(String rawJson) throws MalformedDataException {
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

    private JiraIssue parseSingleIssueStreaming(com.fasterxml.jackson.core.JsonParser parser) throws Exception {
        String id = null;
        String key = null;
        String summary = "";
        String status = "Unknown";
        String issueType = "Unknown";
        String priority = "None";
        String created = "";
        String updated = "";
        String assignee = "Unassigned";

        while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
            String fieldName = parser.currentName();
            if ("id".equals(fieldName)) {
                id = parser.nextTextValue();
            } else if ("key".equals(fieldName)) {
                key = parser.nextTextValue();
            } else if ("fields".equals(fieldName)) {
                if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                    while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
                        String subField = parser.currentName();
                        if ("summary".equals(subField)) {
                            String s = parser.nextTextValue();
                            if (s != null) summary = s;
                        } else if ("status".equals(subField)) {
                            if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                                while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
                                    if ("name".equals(parser.currentName())) {
                                        String val = parser.nextTextValue();
                                        if (val != null) status = val;
                                    } else {
                                        parser.nextToken();
                                        parser.skipChildren();
                                    }
                                }
                            }
                        } else if ("issuetype".equals(subField)) {
                            if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                                while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
                                    if ("name".equals(parser.currentName())) {
                                        String val = parser.nextTextValue();
                                        if (val != null) issueType = val;
                                    } else {
                                        parser.nextToken();
                                        parser.skipChildren();
                                    }
                                }
                            }
                        } else if ("priority".equals(subField)) {
                            if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                                while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
                                    if ("name".equals(parser.currentName())) {
                                        String val = parser.nextTextValue();
                                        if (val != null) priority = val;
                                    } else {
                                        parser.nextToken();
                                        parser.skipChildren();
                                    }
                                }
                            }
                        } else if ("created".equals(subField)) {
                            String val = parser.nextTextValue();
                            if (val != null) created = val;
                        } else if ("updated".equals(subField)) {
                            String val = parser.nextTextValue();
                            if (val != null) updated = val;
                        } else if ("assignee".equals(subField)) {
                            if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                                while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
                                    if ("displayName".equals(parser.currentName())) {
                                        String val = parser.nextTextValue();
                                        if (val != null) assignee = val;
                                    } else {
                                        parser.nextToken();
                                        parser.skipChildren();
                                    }
                                }
                            }
                        } else {
                            parser.nextToken();
                            parser.skipChildren();
                        }
                    }
                }
            } else {
                parser.nextToken();
                parser.skipChildren();
            }
        }

        if (id == null || key == null || id.isBlank() || key.isBlank()) {
            return null;
        }

        return new JiraIssue(id, key, summary, status, issueType, priority, created, updated, assignee);
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
