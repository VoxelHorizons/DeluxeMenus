package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.menu.options.MenuItemOptions;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmptySlotClickTest {

    @Test
    void airItemIsClickableWithoutDisplayedItemStack() {
        MenuItem item = mock(MenuItem.class);
        MenuItemOptions options = mock(MenuItemOptions.class);
        when(item.options()).thenReturn(options);
        when(options.slot()).thenReturn(8);
        when(options.playerSlot()).thenReturn(java.util.Optional.empty());
        MenuHolder holder = new MenuHolder(null, null);
        holder.setActiveItems(Set.of(item));

        assertSame(item, holder.getClickTarget(8, 54, true, false, 8));
        assertNull(holder.getClickTarget(9, 54, true, false, 9));
    }

    @Test
    void outsideAndBottomInventoryDoNotTriggerTopSlot() {
        MenuItem item = mock(MenuItem.class);
        MenuItemOptions options = mock(MenuItemOptions.class);
        when(item.options()).thenReturn(options);
        when(options.slot()).thenReturn(8);
        when(options.playerSlot()).thenReturn(java.util.Optional.empty());
        MenuHolder holder = new MenuHolder(null, null);
        holder.setActiveItems(Set.of(item));

        assertNull(holder.getClickTarget(-999, 54, false, false, -999));
        assertNull(holder.getClickTarget(62, 54, false, true, 8));
        assertNull(holder.getClickTarget(54, 54, true, false, 8));
    }

    @Test
    void playerSlotTargetRemainsSupported() {
        MenuItem item = mock(MenuItem.class);
        MenuItemOptions options = mock(MenuItemOptions.class);
        when(item.options()).thenReturn(options);
        when(options.playerSlot()).thenReturn(java.util.Optional.of(4));
        MenuHolder holder = new MenuHolder(null, null);
        holder.setActiveItems(Set.of(item));

        assertSame(item, holder.getClickTarget(58, 54, false, true, 4));
        assertNull(holder.getClickTarget(58, 54, false, true, 5));
    }
}
