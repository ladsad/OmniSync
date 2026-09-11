package com.omnisync.jira.model;

import java.util.Objects;

/**
 * Typed domain representation of a Jira issue.
 */
public record JiraIssue(
        String id,
        String key,
        String summary,
        String status,
        String issueType,
        String priority,
        String created,
        String updated,
        String assignee
) {
    public JiraIssue {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(key, "key must not be null");
        summary = summary != null ? summary : "";
        status = status != null ? status : "Unknown";
        issueType = issueType != null ? issueType : "Unknown";
        priority = priority != null ? priority : "None";
        created = created != null ? created : "";
        updated = updated != null ? updated : "";
        assignee = assignee != null ? assignee : "Unassigned";
    }
}
