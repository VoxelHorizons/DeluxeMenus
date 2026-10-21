package com.extendedclip.deluxemenus.menu;

/**
 * Determines when real inventory contents must be hidden behind a menu overlay.
 * Kept independent of the Bukkit runtime so it can be unit-tested directly.
 */
final class InventoryVisibilityPolicy {
    private InventoryVisibilityPolicy() {
    }

    static boolean shouldHide(boolean explicitFullscreenMode, boolean hasActivePlayerSlot) {
        return explicitFullscreenMode || hasActivePlayerSlot;
    }
}
