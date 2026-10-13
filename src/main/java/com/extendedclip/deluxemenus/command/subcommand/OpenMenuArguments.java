package com.extendedclip.deluxemenus.command.subcommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The /dm open command is privileged, but menu argument values are data.
 * Retain the existing viewer and optional -p:placeholder-player syntax while
 * forwarding any remaining positional values to the named menu arguments.
 *
 * Input includes the menu name as the first element.
 */
final class OpenMenuArguments {
    private final String viewerName;
    private final String placeholderPlayerName;
    private final List<String> values;

    private OpenMenuArguments(String viewerName, String placeholderPlayerName, List<String> values) {
        this.viewerName = viewerName;
        this.placeholderPlayerName = placeholderPlayerName;
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    static OpenMenuArguments parse(List<String> arguments) {
        if (arguments == null || arguments.isEmpty())
            throw new IllegalArgumentException("Menu name is required");

        String viewer = null;
        String placeholder = null;
        List<String> values = new ArrayList<>();
        for (int i = 1; i < arguments.size(); i++) {
            String argument = arguments.get(i);
            if (argument.startsWith("-p:")) {
                if (placeholder != null || argument.length() <= 3)
                    throw new IllegalArgumentException("Specify one nonempty -p:<player> option");
                placeholder = argument.substring(3);
            } else if (i == 1 && viewer == null && values.isEmpty()) {
                viewer = argument;
            } else {
                values.add(argument);
            }
        }

        return new OpenMenuArguments(viewer, placeholder, values);
    }

    String viewerName() { return viewerName; }
    String placeholderPlayerName() { return placeholderPlayerName; }

    /**
     * Match the existing registered-menu command: every argument consumes one
     * token, except the last one, which receives all remaining words.
     */
    Map<String, String> typedArguments(List<String> names) {
        if (names.isEmpty()) {
            if (!values.isEmpty())
                throw new IllegalArgumentException("This menu has no declared args: " + values);
            return null;
        }
        if (values.size() < names.size())
            throw new IllegalArgumentException("Missing menu arguments: expected " + names.size()
                    + ", received " + values.size());

        Map<String, String> typed = new LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            typed.put(names.get(i), i == names.size() - 1
                    ? String.join(" ", values.subList(i, values.size()))
                    : values.get(i));
        }
        return typed;
    }
}
