package com.extendedclip.deluxemenus.utils;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Prevents exceptions thrown by third-party PlaceholderAPI expansions from
 * aborting the DeluxeMenus inventory render task.
 *
 * NoClassDefFoundError and NoSuchMethodError extend LinkageError, not
 * RuntimeException. We intentionally DO NOT catch arbitrary Error instances
 * (OutOfMemoryError, ThreadDeath, VirtualMachineError, etc.).
 */
public final class PlaceholderFailureGuard {

    private PlaceholderFailureGuard() {
    }

    public static String resolve(String original, Supplier<String> parser,
                                 Consumer<Throwable> reportFailure) {
        Objects.requireNonNull(original, "original");
        Objects.requireNonNull(parser, "parser");
        Objects.requireNonNull(reportFailure, "reportFailure");
        try {
            String result = parser.get();
            return result == null ? original : result;
        } catch (RuntimeException | LinkageError failure) {
            reportFailure.accept(failure);
            return original;
        }
    }
}
