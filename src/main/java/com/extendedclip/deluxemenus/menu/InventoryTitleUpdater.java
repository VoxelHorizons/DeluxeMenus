package com.extendedclip.deluxemenus.menu;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

/**
 * Updates an existing inventory window's title without reopening the inventory.
 *
 * <p>Paper's InventoryView#setTitle(String) is deprecated and converts the title
 * through legacy text, losing Adventure font information (used by custom menu
 * graphics). Send the same open-screen packet Paper uses, retaining the current
 * container ID and full native Adventure component, then resync the slots.
 *
 * <p>No dependency on ProtocolLib, PacketEvents, or a compile-time NMS version.
 */
final class InventoryTitleUpdater {
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private InventoryTitleUpdater() {
    }

    static boolean hasChanged(final Component lastTitle, final Component nextTitle) {
        return !nextTitle.equals(lastTitle);
    }

    static boolean update(final @NotNull Player viewer, final @NotNull Inventory expectedInventory,
                          final @NotNull Component title, final @NotNull Logger logger) {
        final InventoryView view = viewer.getOpenInventory();
        if (view.getTopInventory() != expectedInventory
                || !view.getTopInventory().getType().isCreatable()) {
            return false;
        }

        try {
            // Paper retains Adventure font tags during this conversion.
            final Class<?> paperAdventure = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            final Method asVanilla = paperAdventure.getMethod("asVanilla", Component.class);
            final Object nativeTitle = asVanilla.invoke(null, title);

            final Object handle = viewer.getClass().getMethod("getHandle").invoke(viewer);
            final Object container = handle.getClass().getField("containerMenu").get(handle);
            final int containerId = container.getClass().getField("containerId").getInt(container);
            final Object menuType = container.getClass().getMethod("getType").invoke(container);

            final Class<?> nativeMenuType = Class.forName("net.minecraft.world.inventory.MenuType");
            final Class<?> nativeComponent = Class.forName("net.minecraft.network.chat.Component");
            final Class<?> nativePacket = Class.forName("net.minecraft.network.protocol.Packet");
            final Class<?> openScreen = Class.forName("net.minecraft.network.protocol.game.ClientboundOpenScreenPacket");
            final Constructor<?> constructor = openScreen.getConstructor(int.class, nativeMenuType, nativeComponent);
            final Object packet = constructor.newInstance(containerId, menuType, nativeTitle);

            // Do not close/open or replace the InventoryHolder: that would trigger
            // menu lifecycle hooks and could discard a protected inventory snapshot.
            final Field connectionField = handle.getClass().getField("connection");
            final Object connection = connectionField.get(handle);
            connection.getClass().getMethod("send", nativePacket).invoke(connection, packet);

            // The screen packet resets client slots; resync the existing container
            // (including the temporarily hidden player-inventory slots).
            viewer.updateInventory();
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            if (WARNED.compareAndSet(false, true)) {
                logger.log(Level.WARNING,
                        "Cannot refresh inventory titles on this server version; menu items will still refresh", error);
            }
            return false;
        }
    }
}
