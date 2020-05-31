package org.molkex.spring.minimalrest.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Json {
    private static ObjectMapper mapper = new ObjectMapper();

    public static <T> T read(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String write(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
