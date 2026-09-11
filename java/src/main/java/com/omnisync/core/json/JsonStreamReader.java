package com.omnisync.core.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Centralized utility for high-throughput, low-allocation streaming JSON parsing.
 */
public final class JsonStreamReader {

    private JsonStreamReader() {
        // utility class
    }

    /**
     * Reads an array of items under the specified field name token-by-token.
     *
     * @param parser      streaming Jackson parser
     * @param targetField name of array property
     * @param itemParser  parser callback for each item
     * @param <T>         domain model type
     * @return list of parsed domain items (null items returned by itemParser are defensively omitted)
     * @throws IOException if a syntax error occurs
     */
    public static <T> List<T> readArrayField(JsonParser parser, String targetField, ItemParser<T> itemParser)
            throws IOException {
        Objects.requireNonNull(parser, "parser must not be null");
        Objects.requireNonNull(targetField, "targetField must not be null");
        Objects.requireNonNull(itemParser, "itemParser must not be null");

        List<T> results = new ArrayList<>();

        if (parser.currentToken() == null) {
            parser.nextToken();
        }

        if (parser.currentToken() != JsonToken.START_OBJECT) {
            return results;
        }

        while (parser.nextToken() != JsonToken.END_OBJECT && parser.currentToken() != null) {
            String currentName = parser.currentName();
            if (targetField.equals(currentName)) {
                if (parser.nextToken() == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY && parser.currentToken() != null) {
                        if (parser.currentToken() == JsonToken.START_OBJECT) {
                            T item = itemParser.parse(parser);
                            if (item != null) {
                                results.add(item);
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

        return results;
    }

    /**
     * Safely reads the next string value, returning fallback if null or not a string.
     *
     * @param parser   active parser
     * @param fallback default value if null or missing
     * @return parsed string or fallback
     * @throws IOException on I/O error
     */
    public static String readString(JsonParser parser, String fallback) throws IOException {
        String val = parser.nextTextValue();
        return val != null ? val : fallback;
    }

    /**
     * Safely reads the next integer value, returning fallback if not an integer.
     *
     * @param parser   active parser
     * @param fallback default integer
     * @return parsed int or fallback
     * @throws IOException on I/O error
     */
    public static int readInt(JsonParser parser, int fallback) throws IOException {
        parser.nextToken();
        return parser.getValueAsInt(fallback);
    }
}
