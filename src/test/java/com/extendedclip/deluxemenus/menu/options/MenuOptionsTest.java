package com.extendedclip.deluxemenus.menu.options;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuOptionsTest {

    @Test
    void ordinaryMenusDoNotHidePlayerInventoriesByDefault() {
        MenuOptions options = MenuOptions.builder("ordinary", "Ordinary menu").build();
        assertFalse(options.hidePlayerInventory());
    }

    @Test
    void fullscreenInventoryModeSurvivesBuilderCopy() {
        MenuOptions fullscreen = MenuOptions.builder("fullscreen", "Fullscreen menu")
                .hidePlayerInventory(true)
                .build();

        assertTrue(fullscreen.hidePlayerInventory());
        assertTrue(fullscreen.asBuilder().build().hidePlayerInventory());
    }
}
