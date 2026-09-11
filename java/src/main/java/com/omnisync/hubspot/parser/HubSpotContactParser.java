package com.omnisync.hubspot.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisync.core.error.MalformedDataException;
import com.omnisync.hubspot.model.HubSpotContact;

import java.util.ArrayList;
import java.util.List;

/**
 * Defensive JSON parser for transforming HubSpot CRM contact payloads into domain models.
 */
public class HubSpotContactParser {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a HubSpotContactParser with a default Jackson ObjectMapper.
     */
    public HubSpotContactParser() {
        this(new ObjectMapper());
    }

    /**
     * Constructs a HubSpotContactParser with a custom ObjectMapper.
     *
     * @param objectMapper configured Jackson object mapper
     */
    public HubSpotContactParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Parses a raw JSON response string from HubSpot CRM search or list endpoints.
     *
     * @param rawJson response payload string
     * @return parsed results containing typed contacts and cursor metadata
     * @throws MalformedDataException if JSON syntax is invalid or payload structure is corrupt
     */
    public HubSpotSearchResult parseContactResponse(String rawJson) throws MalformedDataException {
        if (rawJson == null || rawJson.isBlank()) {
            throw new MalformedDataException("Empty or null HubSpot JSON payload", rawJson);
        }

        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isObject()) {
                throw new MalformedDataException("HubSpot response root must be a JSON object", rawJson);
            }

            List<HubSpotContact> contacts = new ArrayList<>();
            JsonNode resultsNode = root.path("results");
            if (resultsNode.isArray()) {
                for (JsonNode node : resultsNode) {
                    HubSpotContact contact = parseSingleContact(node);
                    if (contact != null) {
                        contacts.add(contact);
                    }
                }
            }

            JsonNode pagingNode = root.path("paging");
            JsonNode nextNode = pagingNode.path("next");
            String after = nextNode.path("after").asText(null);
            boolean hasMore = after != null && !after.isBlank();

            return new HubSpotSearchResult(contacts, after, hasMore);
        } catch (MalformedDataException e) {
            throw e;
        } catch (Exception e) {
            throw new MalformedDataException("Failed to parse HubSpot response: " + e.getMessage(), rawJson, e);
        }
    }

    /**
     * Parses a single contact node defensively, skipping if mandatory identifier is absent.
     *
     * @param node JSON node representing a contact
     * @return HubSpotContact or null if invalid
     */
    protected HubSpotContact parseSingleContact(JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }

        String id = node.path("id").asText(null);
        if (id == null || id.isBlank()) {
            return null;
        }

        JsonNode properties = node.path("properties");
        String email = properties.path("email").asText("");
        String firstName = properties.path("firstname").asText("");
        String lastName = properties.path("lastname").asText("");
        String company = properties.path("company").asText("");
        String phone = properties.path("phone").asText("");

        String createdAt = node.path("createdAt").asText(properties.path("createdate").asText(""));
        String updatedAt = node.path("updatedAt").asText(properties.path("lastmodifieddate").asText(""));
        boolean archived = node.path("archived").asBoolean(false);

        return new HubSpotContact(id, email, firstName, lastName, company, phone, createdAt, updatedAt, archived);
    }
}
