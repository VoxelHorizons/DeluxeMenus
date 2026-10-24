package com.extendedclip.deluxemenus.listener;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.action.ClickHandler;
import com.extendedclip.deluxemenus.menu.Menu;
import com.extendedclip.deluxemenus.menu.MenuClickDispatcher;
import com.extendedclip.deluxemenus.menu.MenuHolder;
import com.extendedclip.deluxemenus.menu.MenuItem;
import com.extendedclip.deluxemenus.menu.command.RegistrableMenuCommand;
import com.extendedclip.deluxemenus.requirement.RequirementList;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class PlayerListener extends Listener {

    private final Cache<UUID, Long> cache = CacheBuilder.newBuilder().expireAfterWrite(75, TimeUnit.MILLISECONDS).build();

    // This is so dumb. Mojang fix your shit.
    private final Cache<UUID, Long> shiftCache = CacheBuilder.newBuilder().expireAfterWrite(200, TimeUnit.MILLISECONDS).build();

    public PlayerListener(@NotNull final DeluxeMenus plugin) {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCommandExecute(PlayerCommandPreprocessEvent event) {

        final String[] tokens = event.getMessage().substring(1).trim().split("\\s+");
        if (tokens.length == 0 || tokens[0].isEmpty()) return;
        final String[] args = java.util.Arrays.copyOfRange(tokens, 1, tokens.length);
        if (RegistrableMenuCommand.dispatch(event.getPlayer(), tokens[0], args, false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getPlayerInventoryUiStore().restoreIfPresent(event.getPlayer());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (Menu.isInMenu(player)) {
            Menu.closeMenu(plugin, player, false);
        }
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getPlayer();

        if (player.isSleeping()) {
            event.setCancelled(true);
        }

        if (Menu.isInMenu(player)) {
            Menu.closeMenu(plugin, player, true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {

        if (!(event.getPlayer() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getPlayer();

        Menu.getMenuHolder(player).ifPresent(holder -> {
            if (holder.getInventory() != event.getInventory()) {
                return;
            }

            final boolean protectedInventory = holder.getPlayerInventorySnapshot() != null;
            Menu.closeMenu(plugin, player, false);
            // Cleanup only applies to legacy menus. Protected inventories were
            // restored from disk and must never be filtered by menu-item markers.
            if (!protectedInventory) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!Menu.isInMenu(player)) {
                        Menu.cleanInventory(plugin, player);
                    }
                }, 3L);
            }
        });
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && Menu.isInMenu(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (Menu.getMenuHolder(event.getPlayer())
                .map(holder -> holder.getPlayerInventorySnapshot() != null).orElse(false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrop(PlayerDropItemEvent event) {
        if (Menu.getMenuHolder(event.getPlayer())
                .map(holder -> holder.getPlayerInventorySnapshot() != null).orElse(false)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        final Player player = (Player) event.getWhoClicked();

        final Optional<MenuHolder> optionalHolder = Menu.getMenuHolder(player);

        if (optionalHolder.isEmpty()) {
            return;
        }

        final MenuHolder holder = optionalHolder.get();

        if (holder.getMenu().isEmpty()) {
            Menu.closeMenu(plugin, player, true);
        }

        if (holder.isUpdating()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        final MenuItem item;
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            item = holder.getPlayerItem(event.getSlot());
        } else {
            item = holder.getItem(event.getRawSlot());
        }

        if (item == null) {
            return;
        }

        if (this.cache.getIfPresent(player.getUniqueId()) != null) {
            return;
        }

        if (this.shiftCache.getIfPresent(player.getUniqueId()) != null) {
            return;
        }

        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            return;
        }

        if (event.getClick() == ClickType.SHIFT_LEFT) {
            this.shiftCache.put(player.getUniqueId(), System.currentTimeMillis());
        }

        if (handleClick(player, holder, item.options().clickHandler(), item.options().clickRequirements())) {
            return;
        }

        if (event.isShiftClick() && event.isLeftClick()) {
            if (handleClick(player, holder, item.options().shiftLeftClickHandler(), item.options().shiftLeftClickRequirements())) {
                return;
            }
        }

        if (event.isShiftClick() && event.isRightClick()) {
            if (handleClick(player, holder, item.options().shiftRightClickHandler(), item.options().shiftRightClickRequirements())) {
                return;
            }
        }

        if (event.getClick() == ClickType.LEFT) {
            if (handleClick(player, holder, item.options().leftClickHandler(), item.options().leftClickRequirements())) {
                return;
            }
        }

        if (event.getClick() == ClickType.RIGHT) {
            if (handleClick(player, holder, item.options().rightClickHandler(), item.options().rightClickRequirements())) {
                return;
            }
        }

        if (event.getClick() == ClickType.MIDDLE) {
            if (handleClick(player, holder, item.options().middleClickHandler(), item.options().middleClickRequirements())) {
            }
        }
    }

    /**
     * Handles menu click by player
     *
     * @param player       player who clicked
     * @param holder       menu holder
     * @param handler      click handler
     * @param requirements click requirements
     * @return true if click was handled successfully. will ever return false if no click handler was found
     */
    private boolean handleClick(final @NotNull Player player, final @NotNull MenuHolder holder, final @NotNull Optional<ClickHandler> handler, final @NotNull Optional<RequirementList> requirements) {
        return MenuClickDispatcher.dispatch(holder, handler, requirements,
                () -> this.cache.put(player.getUniqueId(), System.currentTimeMillis()));
    }
}
