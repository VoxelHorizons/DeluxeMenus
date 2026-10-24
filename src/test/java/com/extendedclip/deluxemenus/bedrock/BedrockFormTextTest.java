package com.extendedclip.deluxemenus.bedrock;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BedrockFormTextTest {
    @Test
    void stripsPrivateUseGlyphsAndHexButKeepsLegacyColours() {
        assertEquals("§aProfile §lStats", BedrockFormText.sanitize(" §aProfile  §#ff00aa§lStats"));
        assertEquals("§eShop", BedrockFormText.sanitize("§x§f§f§0§0§a§a§eShop"));
        assertEquals("Hi", BedrockFormText.sanitize("󰀀Hi"));
    }

    @Test
    void blankDetectionIgnoresColourCodes() {
        assertTrue(BedrockFormText.isBlank("§f §r"));
        assertTrue(BedrockFormText.isBlank(BedrockFormText.sanitize("")));
        assertFalse(BedrockFormText.isBlank("§fA"));
    }

    @Test
    void buttonLabelAddsLimitedNonBlankLoreLines() {
        assertEquals("§6Warp\n§7Teleport to spawn§r\n§7Cost: 10§r",
                BedrockFormText.buttonLabel("§6Warp", List.of("", "Teleport to spawn", "", "Cost: 10", "Extra"), 2));
        assertEquals("§6Warp", BedrockFormText.buttonLabel("§6Warp", List.of(), 2));
    }

    @Test
    void materialFallbackIsReadable() {
        assertEquals("Diamond Sword", BedrockFormText.materialName("DIAMOND_SWORD"));
        assertEquals("Stone", BedrockFormText.materialName("STONE"));
    }
}
