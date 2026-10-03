package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.DeluxeMenus;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Crash-safe storage for player inventory slots temporarily occupied by menu items.
 *
 * A recovery file is persisted before DeluxeMenus changes a player slot. Restoring is
 * authoritative for only those touched slots: temporary menu items are replaced by the
 * exact original stacks, then the recovery file is removed.
 */
public final class PlayerInventoryUiStore {

    private final DeluxeMenus plugin;
    private final File directory;

    public PlayerInventoryUiStore(final @NotNull DeluxeMenus plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "player-slot-recovery");
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IllegalStateException("Unable to create player slot recovery directory " + directory);
        }
    }

    public Snapshot capture(final @NotNull Player player, final @NotNull Collection<Integer> slots) throws IOException {
        final Map<Integer, ItemStack> originals = new LinkedHashMap<>();
        final PlayerInventory inventory = player.getInventory();

        for (Integer slot : slots) {
            if (slot == null || slot < 0 || slot > 35 || originals.containsKey(slot)) {
                continue;
            }
            originals.put(slot, cloneItem(inventory.getItem(slot)));
        }

        final Snapshot snapshot = new Snapshot(originals);
        writeAtomic(player.getUniqueId(), snapshot);
        return snapshot;
    }

    public Snapshot load(final @NotNull UUID uuid) {
        final File file = file(uuid);
        if (!file.isFile()) {
            return null;
        }

        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        final List<Integer> slots = yaml.getIntegerList("slots");
        if (slots.isEmpty()) {
            plugin.getLogger().warning("Player-slot recovery file " + file.getName()
                    + " contains no slots; leaving it untouched for manual recovery.");
            return null;
        }

        final Map<Integer, ItemStack> originals = new LinkedHashMap<>();
        for (Integer slot : slots) {
            if (slot == null || slot < 0 || slot > 35) {
                plugin.getLogger().warning("Player-slot recovery file " + file.getName()
                        + " contains invalid slot " + slot + "; leaving it untouched.");
                return null;
            }
            originals.put(slot, cloneItem(yaml.getItemStack("items." + slot)));
        }
        return new Snapshot(originals);
    }

    public void restore(final @NotNull Player player, final Snapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        final PlayerInventory inventory = player.getInventory();
        for (Map.Entry<Integer, ItemStack> entry : snapshot.originals.entrySet()) {
            inventory.setItem(entry.getKey(), cloneItem(entry.getValue()));
        }
        player.updateInventory();
    }

    public void restoreAndDelete(final @NotNull Player player, final Snapshot snapshot) {
        try {
            restore(player, snapshot);
            delete(player.getUniqueId());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to restore temporary DeluxeMenus player slots for " + player.getName()
                            + ". Recovery file has been retained.", exception);
        }
    }

    public void restoreIfPresent(final @NotNull Player player) {
        final Snapshot snapshot = load(player.getUniqueId());
        if (snapshot == null) {
            return;
        }

        plugin.getLogger().warning("Recovering temporary DeluxeMenus player slots for "
                + player.getName() + " from an interrupted menu session.");
        restoreAndDelete(player, snapshot);
    }

    public void delete(final @NotNull UUID uuid) {
        final File file = file(uuid);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete player-slot recovery file " + file.getAbsolutePath());
        }
    }

    private void writeAtomic(final UUID uuid, final Snapshot snapshot) throws IOException {
        final File target = file(uuid);
        final File temp = new File(directory, uuid + ".yml.tmp");

        final YamlConfiguration yaml = new YamlConfiguration();
        final List<Integer> slots = new ArrayList<>(snapshot.originals.keySet());
        yaml.set("slots", slots);
        for (Map.Entry<Integer, ItemStack> entry : snapshot.originals.entrySet()) {
            yaml.set("items." + entry.getKey(), cloneItem(entry.getValue()));
        }
        yaml.save(temp);

        try {
            Files.move(temp.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private File file(final UUID uuid) {
        return new File(directory, uuid + ".yml");
    }

    private static ItemStack cloneItem(final ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        return item.clone();
    }

    public static final class Snapshot {
        private final Map<Integer, ItemStack> originals;

        private Snapshot(final Map<Integer, ItemStack> originals) {
            this.originals = originals;
        }

        public Collection<Integer> slots() {
            return originals.keySet();
        }
    }
}
