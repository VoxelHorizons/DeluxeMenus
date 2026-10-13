package com.extendedclip.deluxemenus.utils;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression tests for /profile <player> alongside Citizens' distinct {npc}.
 * ParseOther uses paired braces to escape the player argument inside PAPI.
 */
class NamedPlayerArgumentTest {
    @Test
    void profileNameResolvesInLabelAndPortrait() {
        Map<String, String> args = Collections.singletonMap("player", "Alex");
        assertEquals("Alexs ᴘʀᴏғɪʟᴇ",
                StringUtils.replaceArguments("{player}s ᴘʀᴏғɪʟᴇ", args, null, false));
        assertEquals("head-Alex",
                StringUtils.replaceArguments("head-{player}", args, null, false));
    }

    @Test
    void nestedParseOtherPlaceholderPreservesOuterBracesForPapi() {
        Map<String, String> args = Collections.singletonMap("player", "Alex");
        assertEquals("%parseother_unsafe_{Alex}_{logic_rank-staff}%",
                StringUtils.replaceArguments(
                        "%parseother_unsafe_{{player}}_{logic_rank-staff}%",
                        args, null, false));
        assertEquals("%parseother_unsafe_{Alex}_{vault_eco_balance_commas}%",
                StringUtils.replaceArguments(
                        "%parseother_unsafe_{{player}}_{vault_eco_balance_commas}%",
                        args, null, false));
    }

    @Test
    void npcArgumentRemainsIndependentOfPlayerArgument() {
        Map<String, String> args = Map.of("player", "Alex", "npc", "42");
        assertEquals("citizenshead-42",
                StringUtils.replaceArguments("citizenshead-{npc}", args, null, false));
        assertEquals("head-Alex",
                StringUtils.replaceArguments("head-{player}", args, null, false));
    }
}
