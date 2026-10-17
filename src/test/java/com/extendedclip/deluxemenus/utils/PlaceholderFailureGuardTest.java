package com.extendedclip.deluxemenus.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderFailureGuardTest {

    @Test
    void noClassDefFoundErrorDoesNotAbortMenuTextRendering() {
        List<Throwable> failures = new ArrayList<>();
        String unresolved = "%mcmmoexpansionpro_mining_skill%";
        String result = PlaceholderFailureGuard.resolve(unresolved,
                () -> { throw new NoClassDefFoundError("com/gmail/nossr50/util/random/RandomChanceUtil"); },
                failures::add);

        assertEquals(unresolved, result);
        assertEquals(1, failures.size());
        assertTrue(failures.get(0) instanceof NoClassDefFoundError);

        // The next menu lore line should still be processed.
        assertEquals("Mining level: 40",
                PlaceholderFailureGuard.resolve("Mining level: %mcmmo_level_mining%",
                        () -> "Mining level: 40", failures::add));
        assertEquals(1, failures.size());
    }

    @Test
    void missingMethodInExpansionIsHandledLikewise() {
        List<Throwable> failures = new ArrayList<>();
        String original = "%broken_expansion%";
        assertEquals(original, PlaceholderFailureGuard.resolve(original,
                () -> { throw new NoSuchMethodError("removed mcMMO method"); }, failures::add));
        assertEquals(1, failures.size());
    }

    @Test
    void ordinaryExpansionExceptionsAreHandled() {
        List<Throwable> failures = new ArrayList<>();
        assertEquals("%broken%", PlaceholderFailureGuard.resolve("%broken%",
                () -> { throw new IllegalStateException("invalid expansion state"); },
                failures::add));
        assertEquals(1, failures.size());
    }

    @Test
    void normalPlaceholderOutputAndNullOutputAreSupported() {
        List<Throwable> failures = new ArrayList<>();
        assertEquals("42", PlaceholderFailureGuard.resolve("%value%", () -> "42", failures::add));
        assertEquals("%value%", PlaceholderFailureGuard.resolve("%value%", () -> null, failures::add));
        assertTrue(failures.isEmpty());
    }

    @Test
    void fatalVirtualMachineErrorsAreNeverSuppressed() {
        List<Throwable> failures = new ArrayList<>();
        OutOfMemoryError fatal = new OutOfMemoryError("test only");
        OutOfMemoryError caught = assertThrows(OutOfMemoryError.class,
                () -> PlaceholderFailureGuard.resolve("%value%", () -> { throw fatal; },
                        failures::add));
        assertSame(fatal, caught);
        assertTrue(failures.isEmpty());
    }
}
