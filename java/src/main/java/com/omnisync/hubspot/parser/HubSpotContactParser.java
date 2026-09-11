package com.omnisync.hubspot.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnisync.core.error.MalformedDataException;
import com.omnisync.core.json.JsonParsingPipeline;
import com.omnisync.hubspot.model.HubSpotContact;

import java.util.ArrayList;
import java.util.List;

/**
 * Defensive JSON parser for transforming HubSpot CRM contact payloads into domain models.
 */
public class HubSpotContactParser {

    private final ObjectMapper objectMapper;

    private final JsonParsingPipeline pipeline;

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
        this.pipeline = new JsonParsingPipeline(this.objectMapper.getFactory());
    }

    /**
     * Parses a raw JSON response string from HubSpot CRM search or list endpoints using streaming.
     *
     * @param rawJson response payload string
     * @return parsed results containing typed contacts and cursor metadata
     * @throws MalformedDataException if JSON syntax is invalid or payload structure is corrupt
     */
    public HubSpotSearchResult parseContactResponse(String rawJson) throws MalformedDataException {
        return parseContactResponseStreaming(rawJson);
    }

    /**
     * High-throughput streaming parser reading contacts token-by-token.
     *
     * @param rawJson response payload string
     * @return parsed results containing typed contacts and cursor metadata
     * @throws MalformedDataException if JSON syntax is invalid or payload structure is corrupt
     */
    public HubSpotSearchResult parseContactResponseStreaming(String rawJson) throws MalformedDataException {
        return pipeline.parseString(rawJson, parser -> {
            if (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                throw new MalformedDataException("HubSpot response root must be a JSON object", rawJson);
            }

            List<HubSpotContact> contacts = new ArrayList<>();
            String after = null;

            while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
                String fieldName = parser.currentName();
                if ("results".equals(fieldName)) {
                    if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_ARRAY) {
                        while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_ARRAY && parser.currentToken() != null) {
                            if (parser.currentToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                                HubSpotContact contact = parseSingleContactStreaming(parser);
                                if (contact != null) {
                                    contacts.add(contact);
                                }
                            } else {
                                parser.skipChildren();
                            }
                        }
                    }
                } else if ("paging".equals(fieldName)) {
                    if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                        while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
                            if ("next".equals(parser.currentName())) {
                                if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                                    while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
                                        if ("after".equals(parser.currentName())) {
                                            after = parser.nextTextValue();
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

            boolean hasMore = after != null && !after.isBlank();
            return new HubSpotSearchResult(contacts, after, hasMore);
        });
    }

    /**
     * Baseline DOM-based parser utilizing ObjectMapper readTree.
     *
     * @param rawJson response payload string
     * @return parsed search result
     * @throws MalformedDataException if JSON syntax is invalid
     */
    public HubSpotSearchResult parseContactResponseDom(String rawJson) throws MalformedDataException {
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

    private HubSpotContact parseSingleContactStreaming(com.fasterxml.jackson.core.JsonParser parser) throws Exception {
        String id = null;
        String createdAt = null;
        String updatedAt = null;
        boolean archived = false;

        String email = "";
        String firstName = "";
        String lastName = "";
        String company = "";
        String phone = "";
        String propCreatedAt = null;
        String propUpdatedAt = null;

        while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
            String fieldName = parser.currentName();
            if ("id".equals(fieldName)) {
                id = parser.nextTextValue();
            } else if ("createdAt".equals(fieldName)) {
                createdAt = parser.nextTextValue();
            } else if ("updatedAt".equals(fieldName)) {
                updatedAt = parser.nextTextValue();
            } else if ("archived".equals(fieldName)) {
                parser.nextToken();
                archived = parser.getValueAsBoolean(false);
            } else if ("properties".equals(fieldName)) {
                if (parser.nextToken() == com.fasterxml.jackson.core.JsonToken.START_OBJECT) {
                    while (parser.nextToken() != com.fasterxml.jackson.core.JsonToken.END_OBJECT && parser.currentToken() != null) {
                        String prop = parser.currentName();
                        if ("email".equals(prop)) {
                            String s = parser.nextTextValue();
                            if (s != null) email = s;
                        } else if ("firstname".equals(prop)) {
                            String s = parser.nextTextValue();
                            if (s != null) firstName = s;
                        } else if ("lastname".equals(prop)) {
                            String s = parser.nextTextValue();
                            if (s != null) lastName = s;
                        } else if ("company".equals(prop)) {
                            String s = parser.nextTextValue();
                            if (s != null) company = s;
                        } else if ("phone".equals(prop)) {
                            String s = parser.nextTextValue();
                            if (s != null) phone = s;
                        } else if ("createdate".equals(prop)) {
                            propCreatedAt = parser.nextTextValue();
                        } else if ("lastmodifieddate".equals(prop)) {
                            propUpdatedAt = parser.nextTextValue();
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

        if (id == null || id.isBlank()) {
            return null;
        }

        String finalCreated = createdAt != null ? createdAt : (propCreatedAt != null ? propCreatedAt : "");
        String finalUpdated = updatedAt != null ? updatedAt : (propUpdatedAt != null ? propUpdatedAt : "");

        return new HubSpotContact(id, email, firstName, lastName, company, phone, finalCreated, finalUpdated, archived);
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
