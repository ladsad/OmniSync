package com.omnisync.core.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.omnisync.core.error.MalformedDataException;

import java.io.InputStream;
import java.util.Objects;

/**
 * Centralized JSON parsing pipeline executing low-allocation streaming transformations.
 */
public class JsonParsingPipeline {

    private final JsonFactory jsonFactory;

    public JsonParsingPipeline() {
        this(new JsonFactory());
    }

    public JsonParsingPipeline(JsonFactory jsonFactory) {
        this.jsonFactory = Objects.requireNonNull(jsonFactory, "jsonFactory must not be null");
    }

    /**
     * Functional extractor contract for converting raw tokens into a parsed result DTO.
     *
     * @param <R> result type
     */
    @FunctionalInterface
    public interface Extractor<R> {
        R extract(JsonParser parser) throws Exception;
    }

    /**
     * Executes a streaming parse operation over a raw JSON string.
     *
     * @param rawJson   raw JSON response string
     * @param extractor custom streaming extractor callback
     * @param <R>       return type
     * @return extracted result
     * @throws MalformedDataException if JSON is null, blank, or malformed
     */
    public <R> R parseString(String rawJson, Extractor<R> extractor) throws MalformedDataException {
        if (rawJson == null || rawJson.isBlank()) {
            throw new MalformedDataException("Empty or null JSON payload", rawJson);
        }

        try (JsonParser parser = jsonFactory.createParser(rawJson)) {
            return extractor.extract(parser);
        } catch (MalformedDataException e) {
            throw e;
        } catch (Exception e) {
            throw new MalformedDataException("Failed to stream JSON payload: " + e.getMessage(), rawJson, e);
        }
    }

    /**
     * Executes a streaming parse operation over an InputStream.
     *
     * @param inputStream byte stream containing JSON data
     * @param extractor   custom streaming extractor callback
     * @param <R>         return type
     * @return extracted result
     * @throws MalformedDataException if input is null or malformed
     */
    public <R> R parseStream(InputStream inputStream, Extractor<R> extractor) throws MalformedDataException {
        if (inputStream == null) {
            throw new MalformedDataException("Null JSON InputStream", "");
        }

        try (JsonParser parser = jsonFactory.createParser(inputStream)) {
            return extractor.extract(parser);
        } catch (MalformedDataException e) {
            throw e;
        } catch (Exception e) {
            throw new MalformedDataException("Failed to stream JSON input stream: " + e.getMessage(), "", e);
        }
    }

    public JsonFactory getJsonFactory() {
        return jsonFactory;
    }
}
