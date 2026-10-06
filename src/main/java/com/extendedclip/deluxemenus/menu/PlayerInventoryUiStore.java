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
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Crash-safe storage for player inventory state temporarily hidden by a DeluxeMenus UI.
 *
 * A single snapshot owns the complete player inventory. This deliberately covers more
 * than the player_slot positions so normal inventory items can never visually overlap
 * a menu item or be accidentally moved/duplicated while the UI is active.
 */
public final class PlayerInventoryUiStore {

    private final DeluxeMenus plugin;
    private final File directory;

    public PlayerInventoryUiStore(final @NotNull DeluxeMenus plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "player-inventory-recovery");
        if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory()) {
            throw new IllegalStateException("Unable to create player inventory recovery directory " + directory);
        }
    }

    public Snapshot capture(final @NotNull Player player) throws IOException {
        final PlayerInventory inventory = player.getInventory();

        final ItemStack[] contents = cloneItems(inventory.getContents());
        final ItemStack[] armor = cloneItems(inventory.getArmorContents());
        final ItemStack[] extra = cloneItems(inventory.getExtraContents());
        final int heldSlot = inventory.getHeldItemSlot();

        final Snapshot snapshot = new Snapshot(contents, armor, extra, heldSlot);
        writeAtomic(player.getUniqueId(), snapshot);
        return snapshot;
    }

    public Snapshot load(final @NotNull UUID uuid) {
        final File file = file(uuid);
        if (!file.isFile()) {
            return null;
        }

        final YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        final int contentsSize = yaml.getInt("contents-size", -1);
        final int armorSize = yaml.getInt("armor-size", -1);
        final int extraSize = yaml.getInt("extra-size", -1);

        if (contentsSize < 0 || armorSize < 0 || extraSize < 0) {
            plugin.getLogger().warning("Player inventory recovery file " + file.getName()
                    + " is invalid; leaving it untouched for manual recovery.");
            return null;
        }

        final ItemStack[] contents = readItems(yaml, "contents", contentsSize);
        final ItemStack[] armor = readItems(yaml, "armor", armorSize);
        final ItemStack[] extra = readItems(yaml, "extra", extraSize);
        final int heldSlot = Math.max(0, Math.min(8, yaml.getInt("held-slot", 0)));

        return new Snapshot(contents, armor, extra, heldSlot);
    }

    public void hide(final @NotNull Player player) {
        final PlayerInventory inventory = player.getInventory();

        inventory.setContents(new ItemStack[inventory.getContents().length]);
        inventory.setArmorContents(new ItemStack[inventory.getArmorContents().length]);
        inventory.setExtraContents(new ItemStack[inventory.getExtraContents().length]);
        player.updateInventory();
    }

    public void restore(final @NotNull Player player, final Snapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        final PlayerInventory inventory = player.getInventory();
        inventory.setContents(cloneItems(snapshot.contents));
        inventory.setArmorContents(cloneItems(snapshot.armor));
        inventory.setExtraContents(cloneItems(snapshot.extra));
        inventory.setHeldItemSlot(snapshot.heldSlot);
        player.updateInventory();
    }

    public void restoreAndDelete(final @NotNull Player player, final Snapshot snapshot) {
        try {
            restore(player, snapshot);
            delete(player.getUniqueId());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to restore temporary DeluxeMenus player inventory for " + player.getName()
                            + ". Recovery file has been retained.", exception);
        }
    }

    public Snapshot restoreIfPresent(final @NotNull Player player) {
        final Snapshot snapshot = load(player.getUniqueId());
        if (snapshot == null) {
            return null;
        }

        plugin.getLogger().warning("Recovering temporary DeluxeMenus player inventory for "
                + player.getName() + " from an interrupted menu session.");
        restoreAndDelete(player, snapshot);
        return snapshot;
    }

    public boolean hasRecovery(final @NotNull UUID uuid) {
        return file(uuid).isFile();
    }

    public void delete(final @NotNull UUID uuid) {
        final File file = file(uuid);
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete player inventory recovery file " + file.getAbsolutePath());
        }
    }

    private void writeAtomic(final UUID uuid, final Snapshot snapshot) throws IOException {
        final File target = file(uuid);
        final File temp = new File(directory, uuid + ".yml.tmp");

        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("contents-size", snapshot.contents.length);
        yaml.set("armor-size", snapshot.armor.length);
        yaml.set("extra-size", snapshot.extra.length);
        yaml.set("held-slot", snapshot.heldSlot);

        writeItems(yaml, "contents", snapshot.contents);
        writeItems(yaml, "armor", snapshot.armor);
        writeItems(yaml, "extra", snapshot.extra);
        yaml.save(temp);

        try {
            Files.move(temp.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void writeItems(final YamlConfiguration yaml, final String path, final ItemStack[] items) {
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].getType() != Material.AIR) {
                yaml.set(path + "." + i, items[i].clone());
            }
        }
    }

    private ItemStack[] readItems(final YamlConfiguration yaml, final String path, final int size) {
        final ItemStack[] items = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            ItemStack item = yaml.getItemStack(path + "." + i);
            items[i] = cloneItem(item);
        }
        return items;
    }

    private File file(final UUID uuid) {
        return new File(directory, uuid + ".yml");
    }

    private static ItemStack[] cloneItems(final ItemStack[] items) {
        if (items == null) {
            return new ItemStack[0];
        }

        final ItemStack[] result = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            result[i] = cloneItem(items[i]);
        }
        return result;
    }

    private static ItemStack cloneItem(final ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return null;
        }
        return item.clone();
    }

    public static final class Snapshot {
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final ItemStack[] extra;
        private final int heldSlot;

        private Snapshot(
                final ItemStack[] contents,
                final ItemStack[] armor,
                final ItemStack[] extra,
                final int heldSlot
        ) {
            this.contents = cloneItems(contents);
            this.armor = cloneItems(armor);
            this.extra = cloneItems(extra);
            this.heldSlot = heldSlot;
        }
    }
}
