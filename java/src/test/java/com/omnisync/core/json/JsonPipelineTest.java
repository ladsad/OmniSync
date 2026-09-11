package com.omnisync.core.json;

import com.fasterxml.jackson.core.JsonToken;
import com.omnisync.core.error.MalformedDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonPipelineTest {

    private final JsonParsingPipeline pipeline = new JsonParsingPipeline();

    @Test
    @DisplayName("parseString extracts structured result from valid JSON")
    void parseStringExtractsResult() {
        String json = "{\"name\": \"OmniSync\", \"count\": 42}";
        String name = pipeline.parseString(json, parser -> {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return null;
            }
            String extracted = null;
            while (parser.nextToken() != JsonToken.END_OBJECT && parser.currentToken() != null) {
                if ("name".equals(parser.currentName())) {
                    parser.nextToken();
                    extracted = parser.getText();
                } else {
                    parser.nextToken();
                    parser.skipChildren();
                }
            }
            return extracted;
        });

        assertThat(name).isEqualTo("OmniSync");
    }

    @Test
    @DisplayName("parseStream extracts result from InputStream")
    void parseStreamExtractsResult() {
        InputStream stream = new ByteArrayInputStream("{\"val\": 99}".getBytes(StandardCharsets.UTF_8));
        int val = pipeline.parseStream(stream, parser -> {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return -1;
            }
            int result = 0;
            while (parser.nextToken() != JsonToken.END_OBJECT && parser.currentToken() != null) {
                if ("val".equals(parser.currentName())) {
                    parser.nextToken();
                    result = parser.getIntValue();
                } else {
                    parser.nextToken();
                    parser.skipChildren();
                }
            }
            return result;
        });

        assertThat(val).isEqualTo(99);
    }

    @Test
    @DisplayName("parseString and parseStream throw MalformedDataException on null or corrupt input")
    void throwsOnCorruptInput() {
        assertThatThrownBy(() -> pipeline.parseString(null, p -> null))
                .isInstanceOf(MalformedDataException.class);

        assertThatThrownBy(() -> pipeline.parseString("   ", p -> null))
                .isInstanceOf(MalformedDataException.class);

        assertThatThrownBy(() -> pipeline.parseStream(null, p -> null))
                .isInstanceOf(MalformedDataException.class);

        assertThatThrownBy(() -> pipeline.parseString("{unclosed-json", p -> {
            p.nextToken();
            p.nextToken();
            return null;
        })).isInstanceOf(MalformedDataException.class);
    }

    @Test
    @DisplayName("JsonStreamReader reads array and ignores extra fields")
    void jsonStreamReaderReadsArray() throws Exception {
        String json = """
                {
                  "ignoredHeader": "test",
                  "items": [
                    {"id": "1"},
                    {"id": "2"},
                    "not-an-object",
                    {"id": "skip-me"}
                  ]
                }
                """;

        List<String> ids = pipeline.parseString(json, parser ->
                JsonStreamReader.readArrayField(parser, "items", itemParser -> {
                    String id = null;
                    while (itemParser.nextToken() != JsonToken.END_OBJECT && itemParser.currentToken() != null) {
                        if ("id".equals(itemParser.currentName())) {
                            id = itemParser.nextTextValue();
                        } else {
                            itemParser.nextToken();
                            itemParser.skipChildren();
                        }
                    }
                    return "skip-me".equals(id) ? null : id;
                })
        );

        assertThat(ids).containsExactly("1", "2");
    }

    @Test
    @DisplayName("JsonStreamReader safe primitive helpers return values or defaults")
    void jsonStreamReaderPrimitiveHelpers() throws Exception {
        String json = "{\"text\": \"hello\", \"number\": 123}";
        pipeline.parseString(json, parser -> {
            parser.nextToken(); // START_OBJECT
            parser.nextToken(); // FIELD_NAME "text"
            String text = JsonStreamReader.readString(parser, "fallback");
            assertThat(text).isEqualTo("hello");

            parser.nextToken(); // FIELD_NAME "number"
            int number = JsonStreamReader.readInt(parser, -1);
            assertThat(number).isEqualTo(123);
            return null;
        });
    }

    @Test
    @DisplayName("JsonStreamReader returns empty list when root is not an object")
    void jsonStreamReaderReturnsEmptyOnNonObject() throws Exception {
        String json = "[\"item1\", \"item2\"]";
        List<String> list = pipeline.parseString(json, parser ->
                JsonStreamReader.readArrayField(parser, "items", p -> null)
        );
        assertThat(list).isEmpty();
    }
}
