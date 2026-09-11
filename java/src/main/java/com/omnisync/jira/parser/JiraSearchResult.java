package com.omnisync.jira.parser;

import com.omnisync.jira.model.JiraIssue;

import java.util.Collections;
import java.util.List;

/**
 * Parsed representation of a Jira search query response.
 */
public record JiraSearchResult(
        int startAt,
        int maxResults,
        int total,
        List<JiraIssue> issues
) {
    public JiraSearchResult {
        issues = issues != null ? List.copyOf(issues) : Collections.emptyList();
    }
}
