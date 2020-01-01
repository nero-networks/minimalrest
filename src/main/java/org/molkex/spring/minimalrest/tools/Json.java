package org.molkex.spring.minimalrest.tools;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Json {

    public static <T> T read(String json, Class<T> type) {
        try {
            return new ObjectMapper().readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String write(Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
