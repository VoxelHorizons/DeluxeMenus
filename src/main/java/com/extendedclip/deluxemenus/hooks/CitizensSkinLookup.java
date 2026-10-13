package com.extendedclip.deluxemenus.hooks;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * Citizens integration boundary. No Citizens classes are linked at class-load
 * time: on servers without Citizens this remains completely inert.
 */
final class CitizensSkinLookup {
    private CitizensSkinLookup() {}

    static int parseNpcId(String input) {
        if (input == null || !input.matches("[0-9]{1,10}"))
            throw new IllegalArgumentException("Expected numeric Citizens NPC ID");
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Citizens NPC ID out of range", error);
        }
    }

    /** Retains the full encoded skin property (rather than a username lookup). */
    static String textureFromTrait(Object trait) {
        if (trait == null) return null;
        try {
            Object value = trait.getClass().getMethod("getTexture").invoke(trait);
            if (!(value instanceof String)) return null;
            String encoded = ((String)value).trim();
            if (encoded.isEmpty()) return null;
            // Reject corrupt/unset textures so they cannot produce a missing
            // portrait or cause the existing head helper to throw.
            byte[] decoded = Base64.getDecoder().decode(encoded);
            String json = new String(decoded, StandardCharsets.UTF_8);
            if (!json.contains("\"textures\"") || !json.contains("\"SKIN\"") ||
                    !json.contains("\"url\"")) return null;
            return encoded;
        } catch (NoSuchMethodException | IllegalAccessException error) {
            throw new IllegalStateException("Citizens SkinTrait API is incompatible", error);
        } catch (InvocationTargetException error) {
            throw new IllegalStateException("Citizens skin lookup failed", error.getCause());
        } catch (IllegalArgumentException error) {
            return null;
        }
    }

    /** Reads the in-memory Citizens NPC registry without network/profile lookups. */
    static String textureForNpc(int id) {
        try {
            Class<?> citizens = Class.forName("net.citizensnpcs.api.CitizensAPI");
            Class<?> registryApi = Class.forName("net.citizensnpcs.api.npc.NPCRegistry");
            Class<?> npcApi = Class.forName("net.citizensnpcs.api.npc.NPC");
            Class<?> skinTrait = Class.forName("net.citizensnpcs.trait.SkinTrait");

            Object registry = citizens.getMethod("getNPCRegistry").invoke(null);
            if (registry == null) return null;
            Object npc = registryApi.getMethod("getById", int.class).invoke(registry, id);
            if (npc == null) return null;
            Object trait = npcApi.getMethod("getTraitNullable", Class.class).invoke(npc, skinTrait);
            return textureFromTrait(trait);
        } catch (ClassNotFoundException error) {
            // Citizens isn't present; caller will supply an untextured head.
            return null;
        } catch (ReflectiveOperationException error) {
            Throwable cause = error instanceof InvocationTargetException
                    ? ((InvocationTargetException) error).getTargetException() : error;
            throw new IllegalStateException("Cannot read Citizens NPC skin for #" + id, cause);
        }
    }
}
