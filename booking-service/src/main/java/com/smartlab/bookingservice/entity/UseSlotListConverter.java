package com.smartlab.bookingservice.entity;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps the {@code use_slots} TEXT column to a {@code List<UseSlot>} as JSON. A
 * column keeps the slots on the line entity, so they flow through the transition
 * engine and the booking DTOs without a separate child table.
 */
@Converter
public class UseSlotListConverter implements AttributeConverter<List<UseSlot>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Override
    public String convertToDatabaseColumn(List<UseSlot> slots) {
        if (slots == null || slots.isEmpty()) return null;
        try {
            return MAPPER.writeValueAsString(slots);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize use_slots", e);
        }
    }

    @Override
    public List<UseSlot> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<UseSlot>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
