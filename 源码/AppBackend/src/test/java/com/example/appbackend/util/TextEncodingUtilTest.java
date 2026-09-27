package com.example.appbackend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextEncodingUtilTest {

    @Test
    void repairsWindows1252DecodedChineseName() {
        assertEquals("测试学生", TextEncodingUtil.repairUtf8Mojibake("æµ‹è¯•å­¦ç”Ÿ"));
    }

    @Test
    void keepsValidChineseAndAsciiUnchanged() {
        assertEquals("测试学生", TextEncodingUtil.repairUtf8Mojibake("测试学生"));
        assertEquals("test_student", TextEncodingUtil.repairUtf8Mojibake("test_student"));
    }
}
