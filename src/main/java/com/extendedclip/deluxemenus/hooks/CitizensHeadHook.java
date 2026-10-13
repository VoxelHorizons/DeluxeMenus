package com.extendedclip.deluxemenus.hooks;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.cache.SimpleCache;
import com.extendedclip.deluxemenus.utils.SkullUtils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Optional Citizens NPC portrait/skull material: citizenshead-{npc}.
 * Citizens already substitutes <npc> into a numeric ID in click commands;
 * DeluxeMenus' declared menu argument makes that ID available as {npc}.
 */
public final class CitizensHeadHook implements ItemHook, SimpleCache {
    private static final int MAX_CACHED_HEADS = 256;
    private final DeluxeMenus plugin;
    private final Map<Integer, CachedHead> cache = new LinkedHashMap<Integer, CachedHead>(
            MAX_CACHED_HEADS + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, CachedHead> eldest) {
            return size() > MAX_CACHED_HEADS;
        }
    };

    private static final class CachedHead {
        private final String texture;
        private final ItemStack item;

        private CachedHead(String texture, ItemStack item) {
            this.texture = texture;
            this.item = item;
        }
    }

    public CitizensHeadHook(@NotNull DeluxeMenus plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull ItemStack getItem(@NotNull String... arguments) {
        if (arguments.length == 0 || !Bukkit.getPluginManager().isPluginEnabled("Citizens"))
            return plugin.getHead().clone();

        final int id;
        try {
            id = CitizensSkinLookup.parseNpcId(arguments[0]);
        } catch (IllegalArgumentException invalid) {
            plugin.getLogger().warning("Invalid Citizens head NPC ID: " + arguments[0]);
            return plugin.getHead().clone();
        }

        try {
            String texture = CitizensSkinLookup.textureForNpc(id);
            if (texture == null) {
                cache.remove(id);
                // The NPC may be using an uninitialized default skin or may not exist.
                // Never silently display the viewing player's head instead.
                return plugin.getHead().clone();
            }

            CachedHead cached = cache.get(id);
            if (cached != null && cached.texture.equals(texture))
                return cached.item.clone();

            ItemStack result = SkullUtils.getSkullByBase64EncodedTextureUrl(plugin, texture);
            cache.put(id, new CachedHead(texture, result.clone()));
            return result;
        } catch (RuntimeException error) {
            plugin.getLogger().log(Level.WARNING, "Unable to create Citizens portrait for NPC #" + id, error);
            return plugin.getHead().clone();
        }
    }

    @Override
    public boolean itemMatchesIdentifiers(@NotNull ItemStack item, @NotNull String... arguments) {
        // Menu-only preview hook; don't mistake a textured NPC head for a
        // canonical player-owned or recipe-matching item.
        return false;
    }

    @Override
    public String getPrefix() {
        return "citizenshead-";
    }

    @Override
    public void clearCache() {
        cache.clear();
    }
}
