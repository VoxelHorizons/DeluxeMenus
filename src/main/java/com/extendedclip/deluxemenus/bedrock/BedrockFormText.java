package com.extendedclip.deluxemenus.bedrock;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

/**
 * Converts Java menu text into text a Bedrock form can show. Legacy section colour
 * codes are kept (forms render them); private-use glyphs used for Java bitmap-font
 * backgrounds, negative spacing and icons are removed because Bedrock shows them as
 * boxes or controller symbols.
 */
public final class BedrockFormText {
    private BedrockFormText() {
    }

    public static @NotNull String sanitize(final @NotNull String text) {
        final StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); ) {
            final int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            if (isPrivateUse(codePoint)) continue;
            out.appendCodePoint(codePoint);
        }
        // Bedrock has no hex colours: drop §#RRGGBB and §x§R§R§G§G§B§B forms.
        // Collapse runs of whitespace left behind by removed spacing glyphs.
        return out.toString().replaceAll("§#[0-9a-fA-F]{6}", "")
                .replaceAll("§[xX](§[0-9a-fA-F]){6}", "").replaceAll("[ \\t]{2,}", " ").trim();
    }

    /** True when nothing readable remains once colour codes are ignored. */
    public static boolean isBlank(final @NotNull String text) {
        return text.replaceAll("§[0-9a-fk-orA-FK-ORxX]", "").trim().isEmpty();
    }

    /** Button text: name plus up to {@code maxLoreLines} non-blank lore lines, dimmed. */
    public static @NotNull String buttonLabel(final @NotNull String name, final @NotNull List<String> lore,
                                              final int maxLoreLines) {
        final StringBuilder label = new StringBuilder(sanitize(name));
        int added = 0;
        for (final String line : lore) {
            if (added >= maxLoreLines) break;
            final String clean = sanitize(line);
            if (isBlank(clean)) continue;
            label.append("\n§7").append(clean).append("§r");
            added++;
        }
        return label.toString();
    }

    /** Readable fallback for an unnamed item, e.g. {@code DIAMOND_SWORD -> Diamond Sword}. */
    public static @NotNull String materialName(final @NotNull String material) {
        final StringBuilder out = new StringBuilder();
        for (final String word : material.toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static boolean isPrivateUse(final int codePoint) {
        return (codePoint >= 0xE000 && codePoint <= 0xF8FF)
                || (codePoint >= 0xF0000 && codePoint <= 0xFFFFD)
                || (codePoint >= 0x100000 && codePoint <= 0x10FFFD);
    }
}
