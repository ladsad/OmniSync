package com.omnisync.hubspot.parser;

import com.omnisync.hubspot.model.HubSpotContact;

import java.util.Collections;
import java.util.List;

/**
 * Parsed representation of a HubSpot CRM query response.
 */
public record HubSpotSearchResult(
        List<HubSpotContact> results,
        String nextAfter,
        boolean hasMore
) {
    public HubSpotSearchResult {
        results = results != null ? List.copyOf(results) : Collections.emptyList();
    }
}
