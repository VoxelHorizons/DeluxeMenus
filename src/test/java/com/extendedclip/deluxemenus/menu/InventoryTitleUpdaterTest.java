package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.utils.StringUtils;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryTitleUpdaterTest {

    @Test
    void unchangedRenderedTitleDoesNotNeedAClientPacket() {
        Component before = StringUtils.color(":offset_-8:<font:voxel:row_6>&f1</font>");
        Component after = StringUtils.color(":offset_-8:<font:voxel:row_6>&f1</font>");
        assertFalse(InventoryTitleUpdater.hasChanged(before, after));
    }

    @Test
    void changedPlaceholderValueNeedsTitleRefresh() {
        Component before = StringUtils.color("<font:voxel:row_6>&f$0.00</font>");
        Component after = StringUtils.color("<font:voxel:row_6>&f$19.98</font>");
        assertTrue(InventoryTitleUpdater.hasChanged(before, after));
    }

    @Test
    void fontChangesAreNotFlattenedToLegacyStrings() {
        Component rowFive = StringUtils.color("<font:voxel:row_5>&f2</font>");
        Component rowSix = StringUtils.color("<font:voxel:row_6>&f2</font>");
        assertTrue(InventoryTitleUpdater.hasChanged(rowFive, rowSix));
        assertNotEquals(rowFive, rowSix);
    }

    @Test
    void initialNullTitleAlwaysRequiresRendering() {
        assertTrue(InventoryTitleUpdater.hasChanged(null, Component.text("title")));
    }
}
