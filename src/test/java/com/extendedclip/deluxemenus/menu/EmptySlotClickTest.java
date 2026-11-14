package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.menu.options.MenuItemOptions;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


class EmptySlotClickTest {

    @Test
    void airItemIsClickableWithoutDisplayedItemStack() {
        MenuItem item = new MenuItem(null, MenuItemOptions.builder().material("AIR").slot(8).build());
        MenuHolder holder = new MenuHolder(null, null);
        holder.setActiveItems(Set.of(item));

        assertSame(item, holder.getClickTarget(8, 54, true, false, 8));
        assertNull(holder.getClickTarget(9, 54, true, false, 9));
    }

    @Test
    void outsideAndBottomInventoryDoNotTriggerTopSlot() {
        MenuItem item = new MenuItem(null, MenuItemOptions.builder().material("AIR").slot(8).build());
        MenuHolder holder = new MenuHolder(null, null);
        holder.setActiveItems(Set.of(item));

        assertNull(holder.getClickTarget(-999, 54, false, false, -999));
        assertNull(holder.getClickTarget(62, 54, false, true, 8));
        assertNull(holder.getClickTarget(54, 54, true, false, 8));
    }

    @Test
    void playerSlotTargetRemainsSupported() {
        MenuItem item = new MenuItem(null, MenuItemOptions.builder().material("AIR").slot(8).playerSlot(4).build());
        MenuHolder holder = new MenuHolder(null, null);
        holder.setActiveItems(Set.of(item));

        assertSame(item, holder.getClickTarget(58, 54, false, true, 4));
        assertNull(holder.getClickTarget(58, 54, false, true, 5));
    }
}
