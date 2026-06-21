package com.leaderboard.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class EmailMaskingSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        int atIdx = value.indexOf('@');
        if (atIdx <= 0) {
            gen.writeString(value);
            return;
        }
        String localPart = value.substring(0, atIdx);
        String domainPart = value.substring(atIdx);
        String maskedLocal;
        if (localPart.length() <= 2) {
            maskedLocal = "*".repeat(localPart.length());
        } else {
            maskedLocal = localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1);
        }
        gen.writeString(maskedLocal + domainPart);
    }
}
