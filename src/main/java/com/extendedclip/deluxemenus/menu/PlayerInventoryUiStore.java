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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Disk-backed inventory escrow for menus that draw on the player's inventory.
 *
 * The original inventory is saved before any items are hidden. Version 2 snapshots
 * include storage, armor, extra/offhand, cursor and selected hotbar slot. Legacy
 * per-slot recovery files from previous builds remain readable in the same folder.
 */
public final class PlayerInventoryUiStore {

    private static final int FULL_SNAPSHOT_VERSION = 2;

    private final DeluxeMenus plugin;
    private final File directory;

    public PlayerInventoryUiStore(final @NotNull DeluxeMenus plugin) {
        this.plugin = plugin;
        // Keep the original directory so upgrading cannot strand old recovery files.
        this.directory = new File(plugin.getDataFolder(), "player-slot-recovery");
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IllegalStateException("Unable to create player inventory recovery directory " + directory);
        }
    }

    public boolean hasRecovery(final @NotNull UUID uuid) {
        return file(uuid).isFile();
    }

    public Snapshot capture(final @NotNull Player player) throws IOException {
        if (hasRecovery(player.getUniqueId())) {
            throw new IOException("A recovery file already exists for " + player.getName()
                    + "; refusing to replace unsafely stored inventory.");
        }

        final PlayerInventory inventory = player.getInventory();
        final Snapshot snapshot = new Snapshot(
                cloneItems(inventory.getStorageContents()),
                cloneItems(inventory.getArmorContents()),
                cloneItems(inventory.getExtraContents()),
                cloneItem(player.getItemOnCursor()),
                inventory.getHeldItemSlot()
        );
        writeAtomic(player.getUniqueId(), snapshot);
        return snapshot;
    }

    public Snapshot load(final @NotNull UUID uuid) {
        final File file = file(uuid);
        if (!file.isFile()) {
            return null;
        }

        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        if (yaml.getInt("format-version", 1) == FULL_SNAPSHOT_VERSION) {
            final int storageSize = yaml.getInt("storage-size", -1);
            final int armorSize = yaml.getInt("armor-size", -1);
            final int extraSize = yaml.getInt("extra-size", -1);
            final int heldSlot = yaml.getInt("held-slot", -1);

            if (storageSize < 0 || storageSize > 54 || armorSize < 0 || armorSize > 10
                    || extraSize < 0 || extraSize > 10 || heldSlot < 0 || heldSlot > 8) {
                plugin.getLogger().severe("Invalid player inventory recovery file " + file.getName()
                        + "; retaining it for manual recovery.");
                return null;
            }

            return new Snapshot(
                    readItems(yaml, "storage", storageSize),
                    readItems(yaml, "armor", armorSize),
                    readItems(yaml, "extra", extraSize),
                    cloneItem(yaml.getItemStack("cursor")),
                    heldSlot
            );
        }

        // Older plugin builds protected only explicitly configured player_slot indices.
        if (yaml.getInt("format-version", 1) != 1) {
            plugin.getLogger().severe("Unknown recovery version in " + file.getName());
            return null;
        }
        final List<Integer> slots = yaml.getIntegerList("slots");
        if (slots.isEmpty()) {
            plugin.getLogger().severe("Invalid legacy player-slot recovery file " + file.getName());
            return null;
        }
        final Map<Integer, ItemStack> originals = new LinkedHashMap<>();
        for (Integer slot : slots) {
            if (slot == null || slot < 0 || slot > 35 || originals.containsKey(slot)) {
                plugin.getLogger().severe("Invalid legacy recovery slot in " + file.getName());
                return null;
            }
            originals.put(slot, cloneItem(yaml.getItemStack("items." + slot)));
        }
        return new Snapshot(originals);
    }

    public void hide(final @NotNull Player player) {
        final PlayerInventory inventory = player.getInventory();
        inventory.setStorageContents(new ItemStack[inventory.getStorageContents().length]);
        inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
        inventory.setExtraContents(new ItemStack[inventory.getExtraContents().length]);
        player.setItemOnCursor(null);
        player.updateInventory();
    }

    public void restore(final @NotNull Player player, final Snapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        final PlayerInventory inventory = player.getInventory();
        if (snapshot.isFull()) {
            inventory.setStorageContents(cloneItems(snapshot.storage));
            inventory.setArmorContents(cloneItems(snapshot.armor));
            inventory.setExtraContents(cloneItems(snapshot.extra));
            inventory.setHeldItemSlot(snapshot.heldSlot);
            player.setItemOnCursor(cloneItem(snapshot.cursor));
        } else {
            for (Map.Entry<Integer, ItemStack> entry : snapshot.legacySlots.entrySet()) {
                inventory.setItem(entry.getKey(), cloneItem(entry.getValue()));
            }
        }
        player.updateInventory();
    }

    /**
     * The recovery file is only deleted after restoration succeeds.
     * Callers can retain their in-memory snapshot if this returns false.
     */
    public boolean restoreAndDelete(final @NotNull Player player, final Snapshot snapshot) {
        if (snapshot == null) {
            return true;
        }
        try {
            restore(player, snapshot);
            return delete(player.getUniqueId());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to restore DeluxeMenus inventory for "
                    + player.getName() + "; recovery file retained.", exception);
            return false;
        }
    }

    /**
     * Returns false if an unreadable or un-restorable file is present. Never
     * overwrite such a file with a new, potentially empty, inventory snapshot.
     */
    public boolean restoreIfPresent(final @NotNull Player player) {
        if (!hasRecovery(player.getUniqueId())) {
            return true;
        }
        final Snapshot snapshot = load(player.getUniqueId());
        if (snapshot == null) {
            return false;
        }
        plugin.getLogger().warning("Recovering DeluxeMenus player inventory for "
                + player.getName() + " from an interrupted menu session.");
        return restoreAndDelete(player, snapshot);
    }

    public boolean delete(final @NotNull UUID uuid) {
        final File file = file(uuid);
        if (!file.exists() || file.delete()) {
            return true;
        }
        plugin.getLogger().severe("Could not remove player inventory recovery file " + file.getAbsolutePath());
        return false;
    }

    private void writeAtomic(final UUID uuid, final Snapshot snapshot) throws IOException {
        final File target = file(uuid);
        final File temp = new File(directory, uuid + ".yml.tmp");

        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("format-version", FULL_SNAPSHOT_VERSION);
        yaml.set("storage-size", snapshot.storage.length);
        yaml.set("armor-size", snapshot.armor.length);
        yaml.set("extra-size", snapshot.extra.length);
        yaml.set("held-slot", snapshot.heldSlot);
        yaml.set("cursor", cloneItem(snapshot.cursor));
        writeItems(yaml, "storage", snapshot.storage);
        writeItems(yaml, "armor", snapshot.armor);
        writeItems(yaml, "extra", snapshot.extra);
        yaml.save(temp);

        try {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp.toPath(), target.toPath());
        }
    }

    private static void writeItems(final YamlConfiguration yaml, final String path, final ItemStack[] items) {
        for (int i = 0; i < items.length; i++) {
            yaml.set(path + "." + i, cloneItem(items[i]));
        }
    }

    private static ItemStack[] readItems(final YamlConfiguration yaml, final String path, final int size) {
        final ItemStack[] items = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            items[i] = cloneItem(yaml.getItemStack(path + "." + i));
        }
        return items;
    }

    private File file(final UUID uuid) {
        return new File(directory, uuid + ".yml");
    }

    private static ItemStack[] cloneItems(final ItemStack[] items) {
        final ItemStack[] copy = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            copy[i] = cloneItem(items[i]);
        }
        return copy;
    }

    private static ItemStack cloneItem(final ItemStack item) {
        return item == null || item.getType() == Material.AIR ? null : item.clone();
    }

    public static final class Snapshot {
        private final ItemStack[] storage;
        private final ItemStack[] armor;
        private final ItemStack[] extra;
        private final ItemStack cursor;
        private final int heldSlot;
        private final Map<Integer, ItemStack> legacySlots;

        private Snapshot(final ItemStack[] storage, final ItemStack[] armor, final ItemStack[] extra,
                         final ItemStack cursor, final int heldSlot) {
            this.storage = cloneItems(storage);
            this.armor = cloneItems(armor);
            this.extra = cloneItems(extra);
            this.cursor = cloneItem(cursor);
            this.heldSlot = heldSlot;
            this.legacySlots = null;
        }

        private Snapshot(final Map<Integer, ItemStack> originals) {
            this.storage = null;
            this.armor = null;
            this.extra = null;
            this.cursor = null;
            this.heldSlot = 0;
            this.legacySlots = originals;
        }

        public boolean isFull() {
            return legacySlots == null;
        }
    }
}
