package com.extendedclip.deluxemenus.utils;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.junit.jupiter.api.Test;

class FontTagsTest {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private record Run(String text, Style style) { }

    private static Component parse(String text) {
        return FontTags.apply(LEGACY.deserialize(text), text);
    }

    private static List<Run> runs(Component component) {
        List<Run> runs = new ArrayList<>();
        collect(component, Style.empty(), runs);
        return runs;
    }

    private static void collect(Component component, Style parent, List<Run> runs) {
        Style style = component.style().merge(parent, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            runs.add(new Run(text.content(), style));
        }
        for (Component child : component.children()) collect(child, style, runs);
    }

    @Test void scopesFontWithoutMovingBackgroundOrOffsetGlyphs() {
        List<Run> r = runs(parse("\ue001\ue002<font:voxel:shop>Tokens: 100</font>\ue003"));
        assertEquals(List.of("\ue001\ue002", "Tokens: 100", "\ue003"), r.stream().map(Run::text).toList());
        assertEquals(Key.key("minecraft:default"), r.get(0).style.font());
        assertEquals(Key.key("voxel:shop"), r.get(1).style.font());
        assertEquals(Key.key("minecraft:default"), r.get(2).style.font());
    }

    @Test void legacyColourAndResetPersistAcrossFontBoundaries() {
        List<Run> r = runs(parse("§a§lA<font:voxel:shop>B§rC</font>D"));
        assertEquals("ABCD", r.stream().map(Run::text).reduce("", String::concat));
        assertEquals(NamedTextColor.GREEN, r.get(1).style.color());
        assertEquals(TextDecoration.State.TRUE, r.get(1).style.decoration(TextDecoration.BOLD));
        assertEquals(Key.key("voxel:shop"), r.get(2).style.font());
        assertNotEquals(TextDecoration.State.TRUE, r.get(2).style.decoration(TextDecoration.BOLD));
        assertNull(r.get(3).style.color());
    }

    @Test void nestedAndAdjacentFontsRestorePreviousScope() {
        List<Run> r = runs(parse("<font:voxel:shop>A<font:voxel:bottom>B</font>C</font><font:voxel:other>D</font>"));
        assertEquals(List.of("voxel:shop", "voxel:bottom", "voxel:shop", "voxel:other"),
                r.stream().map(v -> v.style.font().asString()).toList());
    }

    @Test void ordinaryAndMalformedTextIsUnchanged() {
        for (String input : List.of("§aNormal text", "<player>", "<font:BAD:shop>X</font>",
                "<font:voxel:shop>X", "X</font>", "</font><font:voxel:shop>X")) {
            assertEquals(LEGACY.deserialize(input), parse(input));
        }
    }

    @Test void keepsUnicodeAndDoesNotEnableOtherMiniMessageTags() {
        List<Run> r = runs(parse("<font:voxel:shop>ᴛᴏᴋᴇɴs 😀 <click:run_command:/op></font>"));
        assertEquals("ᴛᴏᴋᴇɴs 😀 <click:run_command:/op>", r.get(0).text);
        assertNull(r.get(0).style.clickEvent());
    }
}
