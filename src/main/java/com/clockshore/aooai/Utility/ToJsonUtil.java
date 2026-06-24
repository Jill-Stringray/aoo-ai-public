package com.clockshore.aooai.Utility;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;

import java.util.HashMap;
import java.util.Map;

public class ToJsonUtil {

    @Resource
    private ObjectMapper objectMapper;

    public String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize failed", e);
        }
    }

    public <T> T fromJson(
            String json,
            TypeReference<T> typeReference) {

        try {
            return objectMapper.readValue(
                    json,
                    typeReference
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "JSON反序列化失败",
                    e
            );
        }
    }

    public Map<String, Object> toMap(
            String json) {

        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }

        return fromJson(
                json,
                new TypeReference<Map<String, Object>>() {}
        );
    }
}
