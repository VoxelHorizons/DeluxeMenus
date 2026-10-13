package com.extendedclip.deluxemenus.command.subcommand;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenMenuArgumentsTest {
    private static OpenMenuArguments parse(String... commandArguments) {
        return OpenMenuArguments.parse(Arrays.asList(commandArguments));
    }

    @Test void citizensServerCommandForwardsNpcIdToMenu() {
        OpenMenuArguments command = parse("npcprofile", "FlopsiNZ", "42");
        assertEquals("FlopsiNZ", command.viewerName());
        assertNull(command.placeholderPlayerName());
        assertEquals(Collections.singletonMap("npc", "42"),
                command.typedArguments(Collections.singletonList("npc")));
    }

    @Test void existingNoArgumentAdminOpenIsUnchanged() {
        OpenMenuArguments command = parse("shop", "FlopsiNZ");
        assertEquals("FlopsiNZ", command.viewerName());
        assertNull(command.typedArguments(Collections.emptyList()));
    }

    @Test void existingPlaceholderPlayerOverrideWorksWithoutMenuArgs() {
        OpenMenuArguments self = parse("shop", "-p:OtherPlayer");
        assertNull(self.viewerName());
        assertEquals("OtherPlayer", self.placeholderPlayerName());
        assertNull(self.typedArguments(Collections.emptyList()));

        OpenMenuArguments other = parse("shop", "FlopsiNZ", "-p:OtherPlayer");
        assertEquals("FlopsiNZ", other.viewerName());
        assertEquals("OtherPlayer", other.placeholderPlayerName());
        assertNull(other.typedArguments(Collections.emptyList()));
    }

    @Test void acceptsPlaceholderOverrideBeforeOrAfterMenuArguments() {
        OpenMenuArguments first = parse("npcprofile", "FlopsiNZ", "-p:AnotherPlayer", "42");
        OpenMenuArguments last = parse("npcprofile", "FlopsiNZ", "42", "-p:AnotherPlayer");
        for (OpenMenuArguments command : Arrays.asList(first, last)) {
            assertEquals("AnotherPlayer", command.placeholderPlayerName());
            assertEquals("42", command.typedArguments(Collections.singletonList("npc")).get("npc"));
        }
    }

    @Test void lastMenuArgumentConsumesRemainingWordsLikeRegisteredMenuCommands() {
        OpenMenuArguments command = parse("dialogue", "FlopsiNZ", "42", "welcome", "to", "Voxel", "Horizons");
        assertEquals(Map.of("npc", "42", "message", "welcome to Voxel Horizons"),
                command.typedArguments(List.of("npc", "message")));
    }

    @Test void menuCanReceiveMultiplePositionalArguments() {
        OpenMenuArguments command = parse("npcprofile", "FlopsiNZ", "42", "quest");
        assertEquals(Map.of("npc", "42", "mode", "quest"),
                command.typedArguments(List.of("npc", "mode")));
    }

    @Test void failsExplicitlyWhenMenuArgsNotDeclaredOrIncomplete() {
        OpenMenuArguments command = parse("npcprofile", "FlopsiNZ", "42");
        assertThrows(IllegalArgumentException.class,
                () -> command.typedArguments(Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,
                () -> command.typedArguments(List.of("npc", "mode")));
    }

    @Test void rejectsDuplicateOrEmptyPlaceholderOverrides() {
        assertThrows(IllegalArgumentException.class,
                () -> parse("npcprofile", "FlopsiNZ", "-p:", "42"));
        assertThrows(IllegalArgumentException.class,
                () -> parse("npcprofile", "FlopsiNZ", "-p:A", "-p:B", "42"));
    }
}
