package com.extendedclip.deluxemenus.bedrock;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.menu.Menu;
import com.extendedclip.deluxemenus.menu.MenuClickDispatcher;
import com.extendedclip.deluxemenus.menu.MenuHolder;
import com.extendedclip.deluxemenus.menu.MenuItem;
import com.extendedclip.deluxemenus.menu.options.MenuOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

/**
 * Bedrock frontend for DeluxeMenus (VoxelHorizons B15). Bedrock viewers receive a native
 * simple form instead of the Java chest GUI, whose bitmap-font backgrounds and glyph
 * overlays cannot render on Bedrock. The form is built from the same {@link MenuHolder}:
 * open/argument/view requirements, passed arguments and placeholder target already ran,
 * and each button runs the item's own click handler behind its click requirements, so
 * permissions and actions match the Java menu. Java players are never routed here.
 */
public final class BedrockMenuForms {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final int MAX_LORE_LINES = 2;

    private final DeluxeMenus plugin;
    private @Nullable BedrockFormBridge bridge;
    private boolean enabled;

    public BedrockMenuForms(final @NotNull DeluxeMenus plugin) {
        this.plugin = plugin;
    }

    /** Call after plugins are enabled and on reload. */
    public void load(final boolean enabled) {
        this.enabled = enabled;
        this.bridge = enabled ? BedrockFormBridge.detect(plugin) : null;
    }

    public boolean routes(final @NotNull Player viewer, final @NotNull MenuOptions options) {
        final BedrockFormBridge current = bridge;
        return enabled && current != null && options.bedrockForm() && current.isBedrock(viewer);
    }

    /** Present (or re-present) the holder's current state. Main thread only. */
    public void show(final @NotNull MenuHolder holder) {
        final BedrockFormBridge current = bridge;
        final Player viewer = holder.getViewer();
        if (current == null || viewer == null || !viewer.isOnline()) return;
        if (Menu.getMenuHolder(viewer).orElse(null) != holder) return;

        final List<MenuItem> items = new ArrayList<>(holder.getActiveItems());
        items.sort(Comparator.comparingInt((MenuItem item) -> item.options().playerSlot().isPresent() ? 1 : 0)
                .thenComparingInt(item -> item.options().playerSlot().orElse(item.options().slot())));

        final List<MenuItem> buttonItems = new ArrayList<>();
        final List<BedrockFormBridge.Button> buttons = new ArrayList<>();
        final StringBuilder content = new StringBuilder();
        for (final MenuItem item : items) {
            final ItemStack stack;
            try {
                stack = item.getItemStack(holder);
            } catch (RuntimeException failure) {
                plugin.getLogger().log(Level.WARNING, "Skipping Bedrock form item in " + holder.getMenuName(), failure);
                continue;
            }
            if (stack == null) continue;
            final String name = displayName(stack);
            final List<String> lore = lore(stack);
            if (MenuClickDispatcher.hasAnyHandler(item)) {
                buttonItems.add(item);
                final String label = BedrockFormText.isBlank(BedrockFormText.sanitize(name))
                        ? BedrockFormText.materialName(stack.getType().name())
                        : BedrockFormText.buttonLabel(name, lore, MAX_LORE_LINES);
                // Placeholders/arguments allow per-viewer avatars, e.g. a head-render URL with %player_name%.
                final String image = item.options().bedrockImage()
                        .map(holder::setPlaceholdersAndArguments).map(String::trim).orElse(null);
                final String imageType = BedrockFormText.imageType(image);
                buttons.add(new BedrockFormBridge.Button(label, imageType, imageType == null ? null : image));
            } else if (!BedrockFormText.isBlank(BedrockFormText.sanitize(name))) {
                // Display-only items (e.g. profile statistics) become readable content lines.
                content.append(BedrockFormText.buttonLabel(name, lore, Integer.MAX_VALUE)).append("\n\n");
            }
        }

        final String title = holder.getMenu()
                .map(menu -> BedrockFormText.sanitize(LEGACY.serialize(
                        com.extendedclip.deluxemenus.utils.StringUtils.color(
                                holder.setPlaceholdersAndArguments(menu.options().title())))))
                .filter(text -> !BedrockFormText.isBlank(text))
                .orElse(holder.getMenuName());

        final boolean sent = current.send(viewer, title, content.toString().trim(), buttons,
                index -> onButton(holder, buttonItems, index),
                () -> onClosed(holder));
        if (!sent) {
            // Never leave a Bedrock player stuck in an invisible menu.
            Menu.closeMenu(plugin, viewer, false);
        }
    }

    private void onButton(final MenuHolder holder, final List<MenuItem> buttonItems, final int index) {
        final Player viewer = holder.getViewer();
        if (viewer == null || Menu.getMenuHolder(viewer).orElse(null) != holder) return;
        if (index < 0 || index >= buttonItems.size()) {
            show(holder);
            return;
        }
        MenuClickDispatcher.dispatchPrimary(holder, buttonItems.get(index), () -> { });
        // Like a Java menu that stays open after a click: re-present unless the actions
        // closed this menu or opened another one. Two ticks let [refresh] settle.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> show(holder), 2L);
    }

    private void onClosed(final MenuHolder holder) {
        final Player viewer = holder.getViewer();
        if (viewer != null && Menu.getMenuHolder(viewer).orElse(null) == holder) {
            // Same as closing the Java inventory.
            Menu.closeMenu(plugin, viewer, false);
        }
    }

    private static String displayName(final ItemStack stack) {
        final ItemMeta meta = stack.getItemMeta();
        final Component name = meta == null ? null : meta.displayName();
        return name == null ? "" : LEGACY.serialize(name);
    }

    private static List<String> lore(final ItemStack stack) {
        final ItemMeta meta = stack.getItemMeta();
        final List<Component> lore = meta == null ? null : meta.lore();
        final List<String> lines = new ArrayList<>();
        if (lore != null) for (final Component line : lore) lines.add(LEGACY.serialize(line));
        return lines;
    }
}
