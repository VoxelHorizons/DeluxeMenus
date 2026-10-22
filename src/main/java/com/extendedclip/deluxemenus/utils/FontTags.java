package com.extendedclip.deluxemenus.utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

/** Applies only font tags; legacy colour parsing remains the responsibility of StringUtils. */
final class FontTags {
    private static final Pattern TAG = Pattern.compile("<font:([a-z0-9_.-]+:[a-z0-9/._-]+)>|</font>");
    private static final Key DEFAULT_FONT = Key.key("minecraft:default");

    private FontTags() { }

    static Component apply(Component component, String input) {
        Matcher matcher = TAG.matcher(input);
        int depth = 0;
        boolean found = false;
        while (matcher.find()) {
            found = true;
            if (matcher.group(1) != null) {
                depth++;
            } else if (--depth < 0) {
                return component;
            }
        }
        // Preserve the original text if a recognised tag is unmatched.
        if (!found || depth != 0) return component;
        return apply(component, new ArrayDeque<>());
    }

    private static Component apply(Component component, Deque<Key> fonts) {
        List<Component> children = new ArrayList<>();
        Component result = component.children(List.of());
        if (component instanceof TextComponent text) {
            result = ((TextComponent) result).content("");
            Matcher matcher = TAG.matcher(text.content());
            int start = 0;
            while (matcher.find()) {
                append(children, text.content().substring(start, matcher.start()), fonts);
                if (matcher.group(1) != null) {
                    fonts.push(Key.key(matcher.group(1)));
                } else {
                    fonts.pop();
                }
                start = matcher.end();
            }
            append(children, text.content().substring(start), fonts);
        }
        for (Component child : component.children()) children.add(apply(child, fonts));
        return result.children(children);
    }

    private static void append(List<Component> children, String text, Deque<Key> fonts) {
        if (!text.isEmpty()) {
            children.add(Component.text(text).font(fonts.isEmpty() ? DEFAULT_FONT : fonts.peek()));
        }
    }
}
