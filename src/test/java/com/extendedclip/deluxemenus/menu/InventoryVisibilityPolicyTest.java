package com.extendedclip.deluxemenus.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryVisibilityPolicyTest {

    @Test
    void ordinaryMenuDoesNotHideItems() {
        assertFalse(InventoryVisibilityPolicy.shouldHide(false, false));
    }

    @Test
    void fullscreenMenuHidesItemsWithoutPlayerSlotButtons() {
        assertTrue(InventoryVisibilityPolicy.shouldHide(true, false));
    }

    @Test
    void playerSlotButtonsAlwaysHideRemainingPlayerInventory() {
        assertTrue(InventoryVisibilityPolicy.shouldHide(false, true));
        assertTrue(InventoryVisibilityPolicy.shouldHide(true, true));
    }
}
