package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.utils.StringUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

/**
 * Runs optional inventory-title glyph resolution before sending an in-place
 * title update. VoxelCore normally performs this during InventoryOpenEvent,
 * which is deliberately NOT fired by the [refresh] action.
 *
 * <p>The integration is optional and reflective: DeluxeMenus does not depend
 * on VoxelCore at compile time, and does not duplicate glyph allocations.
 */
final class MenuTitlePlaceholderResolver {
    private static boolean warned;

    private MenuTitlePlaceholderResolver() {
    }

    static Component resolve(final Component title, final DeluxeMenus plugin) {
        final Plugin voxelCore = plugin.getServer().getPluginManager().getPlugin("VoxelCore");
        if (voxelCore == null || !voxelCore.isEnabled()) {
            return title;
        }

        try {
            final Object service = voxelCore.getClass().getMethod("getTextPlaceholderService")
                    .invoke(voxelCore);
            if (service == null) {
                return title;
            }
            final Method resolve = service.getClass().getMethod("resolve", String.class);
            return resolveTextLeaves(title, text -> {
                try {
                    final Object result = resolve.invoke(service, text);
                    return result instanceof String ? (String) result : text;
                } catch (IllegalAccessException | InvocationTargetException exception) {
                    throw new IllegalStateException("VoxelCore font resolution failed", exception);
                }
            });
        } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
            if (!warned) {
                warned = true;
                plugin.getLogger().log(Level.WARNING,
                        "Unable to resolve VoxelCore title glyphs during menu refresh", exception);
            }
            return title;
        }
    }

    /**
     * Resolve aliases in individual text leaves, preserving Adventure component
     * styling and custom fonts on all surrounding nodes. Unlike serializing the
     * entire title to legacy text, this does not flatten font/colour boundaries.
     */
    static Component resolveTextLeaves(final Component component,
                                       final UnaryOperator<String> resolver) {
        final List<Component> children = component.children();
        final List<Component> replacements = new ArrayList<>(children.size() + 1);
        Component base = component;
        boolean changed = false;

        if (component instanceof TextComponent text) {
            final String original = text.content();
            if (original.indexOf(':') >= 0) {
                final String resolved = resolver.apply(original);
                if (resolved != null && !Objects.equals(resolved, original)) {
                    // Keep the source node's style so its descendants inherit it.
                    // VoxelCore's colour codes are interpreted only inside the new
                    // leaf, just as in its InventoryOpenEvent title listener.
                    base = text.content("");
                    replacements.add(StringUtils.color(resolved));
                    changed = true;
                }
            }
        }

        for (Component child : children) {
            final Component resolvedChild = resolveTextLeaves(child, resolver);
            replacements.add(resolvedChild);
            changed |= resolvedChild != child;
        }

        return changed ? base.children(replacements) : component;
    }
}
