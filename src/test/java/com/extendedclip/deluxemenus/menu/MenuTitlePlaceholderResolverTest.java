package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.utils.StringUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MenuTitlePlaceholderResolverTest {

    @Test
    void resolvesBackgroundAndOffsetsWithoutFlatteningRowFonts() {
        Component original = StringUtils.color(
                ":offset_-8::server_shop_background:<font:voxel:row_6>&f$4.99</font>:offset_-15:");
        Component resolved = MenuTitlePlaceholderResolver.resolveTextLeaves(original, text -> text
                .replace(":offset_-8:", "")
                .replace(":server_shop_background:", "")
                .replace(":offset_-15:", ""));
        String flattened = plainText(resolved);
        assertFalse(flattened.contains(":offset_"));
        assertFalse(flattened.contains(":server_shop_background:"));
        assertTrue(flattened.contains("$4.99"));

        List<TextComponent> leaves = new ArrayList<>();
        collectText(resolved, leaves);
        assertTrue(leaves.stream().anyMatch(leaf -> leaf.content().contains("$4.99")
                && Key.key("voxel:row_6").equals(leaf.font())));
    }

    @Test
    void preservesColourAndFontWhenGlyphsAreReplaced() {
        Component original = StringUtils.color("<font:voxel:row_8>&cDisclosure</font>:offset_-9:");
        Component resolved = MenuTitlePlaceholderResolver.resolveTextLeaves(original,
                text -> text.replace(":offset_-9:", "§f"));
        assertTrue(plainText(resolved).contains("Disclosure"));
        assertFalse(plainText(resolved).contains(":offset_-9:"));
        assertNotEquals(original, resolved);
    }

    @Test
    void unchangedComponentsKeepIdentityAndSkipUnnecessaryPackets() {
        Component original = StringUtils.color("<font:voxel:row_6>&f1</font>");
        Component resolved = MenuTitlePlaceholderResolver.resolveTextLeaves(original, text -> text);
        assertSame(original, resolved);
    }

    private static String plainText(Component component) {
        StringBuilder builder = new StringBuilder();
        if (component instanceof TextComponent text) builder.append(text.content());
        for (Component child : component.children()) builder.append(plainText(child));
        return builder.toString();
    }

    private static void collectText(Component c, List<TextComponent> leaves) {
        if (c instanceof TextComponent t) leaves.add(t);
        for (Component child : c.children()) collectText(child, leaves);
    }
}
