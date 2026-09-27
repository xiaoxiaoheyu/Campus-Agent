package com.example.appbackend.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Repairs legacy text where UTF-8 bytes were decoded once as Windows-1252. */
public final class TextEncodingUtil {

    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    private TextEncodingUtil() {
    }

    public static String repairUtf8Mojibake(String value) {
        if (value == null || value.isBlank() || !looksLikeUtf8Mojibake(value)) {
            return value;
        }
        try {
            ByteBuffer bytes = WINDOWS_1252.newEncoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .encode(CharBuffer.wrap(value));
            String repaired = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(bytes)
                    .toString();
            return repaired.isBlank() ? value : repaired;
        } catch (CharacterCodingException ignored) {
            return value;
        }
    }

    private static boolean looksLikeUtf8Mojibake(String value) {
        return value.chars().anyMatch(ch -> ch == 'Ã' || ch == 'Â' || ch == 'Æ'
                || ch == 'æ' || ch == 'å' || ch == 'ç' || ch == 'è' || ch == 'é'
                || ch == 'â');
    }
}
