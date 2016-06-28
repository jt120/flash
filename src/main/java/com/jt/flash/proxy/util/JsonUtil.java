package com.jt.flash.proxy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * since 2016/6/22.
 */
public class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }



     public static void writeValue(Writer writer, Object obj) throws IOException {
        Preconditions.checkNotNull(writer);

        try {
            mapper.writeValue(writer, obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("jackson format error: " + obj.getClass(), e);
        }
    }

    public static String writeValueAsString(Object obj) {

        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("jackson format error: " + obj.getClass(), e);
        }
    }

    public static <T> T readValue(String json, Class<T> type) {

        try {
            return (T) mapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException("jackson parse error :" + json.substring(0, Math.min(100, json.length())), e);
        }
    }

    public static <T> T readValue(Reader reader, Class<T> type) throws IOException {

        Preconditions.checkNotNull(reader);

        try {
            return (T) mapper.readValue(reader, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("jackson parse error.", e);
        }
    }

    public static <T> T readValue(String json, TypeReference<T> type) {
        try {
            return (T) mapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException("jackson parse error.", e);
        }
    }

    public static <T> T readValue(Reader reader, TypeReference<T> type) throws IOException {

        Preconditions.checkNotNull(reader);

        try {
            return (T) mapper.readValue(reader, type);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("jackson parse error.", e);
        }
    }


}
