package com.omnisync.hubspot.parser;

import com.omnisync.core.error.MalformedDataException;
import com.omnisync.hubspot.model.HubSpotContact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HubSpotContactParserTest {

    private final HubSpotContactParser parser = new HubSpotContactParser();

    @Test
    @DisplayName("parseContactResponse parses valid contacts and paging cursor")
    void parsesValidResponse() {
        String json = """
                {
                  "results": [
                    {
                      "id": "512",
                      "properties": {
                        "email": "sarah.connor@example.com",
                        "firstname": "Sarah",
                        "lastname": "Connor",
                        "company": "Cyberdyne",
                        "phone": "555-0100"
                      },
                      "createdAt": "2026-09-01T12:00:00.000Z",
                      "updatedAt": "2026-09-02T15:30:00.000Z",
                      "archived": false
                    }
                  ],
                  "paging": {
                    "next": {
                      "after": "cursor-513"
                    }
                  }
                }
                """;

        HubSpotSearchResult result = parser.parseContactResponse(json);

        assertThat(result.hasMore()).isTrue();
        assertThat(result.nextAfter()).isEqualTo("cursor-513");
        assertThat(result.results()).hasSize(1);

        HubSpotContact contact = result.results().get(0);
        assertThat(contact.id()).isEqualTo("512");
        assertThat(contact.email()).isEqualTo("sarah.connor@example.com");
        assertThat(contact.firstName()).isEqualTo("Sarah");
        assertThat(contact.lastName()).isEqualTo("Connor");
        assertThat(contact.company()).isEqualTo("Cyberdyne");
        assertThat(contact.phone()).isEqualTo("555-0100");
        assertThat(contact.createdAt()).isEqualTo("2026-09-01T12:00:00.000Z");
        assertThat(contact.updatedAt()).isEqualTo("2026-09-02T15:30:00.000Z");
        assertThat(contact.archived()).isFalse();
    }

    @Test
    @DisplayName("parseContactResponse defensively skips invalid contact objects and handles terminal page")
    void handlesMissingFieldsAndTerminalPage() {
        String json = """
                {
                  "results": [
                    {
                      "id": "513",
                      "properties": {}
                    },
                    {
                      "invalid": "no id"
                    },
                    null
                  ]
                }
                """;

        HubSpotSearchResult result = parser.parseContactResponse(json);

        assertThat(result.hasMore()).isFalse();
        assertThat(result.nextAfter()).isNull();
        assertThat(result.results()).hasSize(1);

        HubSpotContact contact = result.results().get(0);
        assertThat(contact.id()).isEqualTo("513");
        assertThat(contact.email()).isEmpty();
        assertThat(contact.firstName()).isEmpty();
    }

    @Test
    @DisplayName("parseContactResponse throws MalformedDataException on invalid json")
    void throwsOnInvalidJson() {
        assertThatThrownBy(() -> parser.parseContactResponse("{invalid-json"))
                .isInstanceOf(MalformedDataException.class);

        assertThatThrownBy(() -> parser.parseContactResponse("[]"))
                .isInstanceOf(MalformedDataException.class)
                .hasMessageContaining("root must be a JSON object");

        assertThatThrownBy(() -> parser.parseContactResponse(""))
                .isInstanceOf(MalformedDataException.class);
    }
}
