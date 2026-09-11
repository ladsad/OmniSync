package com.omnisync.core.json;

import com.fasterxml.jackson.core.JsonParser;

import java.io.IOException;

/**
 * Functional interface for parsing a single domain item from a streaming Jackson JsonParser.
 *
 * @param <T> the type of item being parsed
 */
@FunctionalInterface
public interface ItemParser<T> {

    /**
     * Parses a single item from the current position of the JsonParser.
     *
     * @param parser the active streaming JsonParser positioned at or inside the item
     * @return parsed domain item, or null if item is malformed and should be skipped
     * @throws IOException if a low-level I/O or syntax error occurs
     */
    T parse(JsonParser parser) throws IOException;
}
