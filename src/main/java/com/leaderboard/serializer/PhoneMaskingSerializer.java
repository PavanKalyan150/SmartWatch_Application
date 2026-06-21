package com.leaderboard.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class PhoneMaskingSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        if (value.length() <= 4) {
            gen.writeString("*".repeat(value.length()));
        } else {
            String lastFour = value.substring(value.length() - 4);
            gen.writeString("*".repeat(value.length() - 4) + lastFour);
        }
    }
}
