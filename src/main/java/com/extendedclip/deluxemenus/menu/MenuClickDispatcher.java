package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.action.ClickHandler;
import com.extendedclip.deluxemenus.requirement.RequirementList;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Runs a menu item's click handler behind its click requirements. Shared by the Java
 * inventory listener and the Bedrock form frontend so both enforce identical
 * permissions, requirements and deny actions.
 */
public final class MenuClickDispatcher {
    private MenuClickDispatcher() {
    }

    /**
     * @param beforeRun invoked just before the handler runs (e.g. click-spam cooldown)
     * @return true if a handler existed (whether it ran or was denied)
     */
    public static boolean dispatch(final @NotNull MenuHolder holder, final @NotNull Optional<ClickHandler> handler,
                                   final @NotNull Optional<RequirementList> requirements,
                                   final @NotNull Runnable beforeRun) {
        if (handler.isEmpty()) {
            return false;
        }

        if (requirements.isPresent()) {
            final ClickHandler denyHandler = requirements.get().getDenyHandler();

            if (!requirements.get().evaluate(holder)) {
                if (denyHandler != null) {
                    denyHandler.onClick(holder);
                }
                return true;
            }
        }

        beforeRun.run();
        handler.get().onClick(holder);
        return true;
    }

    /**
     * Bedrock form buttons have one kind of press. Use the item's generic click handler,
     * then left, right, middle, shift-left and shift-right, in that order.
     */
    public static boolean dispatchPrimary(final @NotNull MenuHolder holder, final @NotNull MenuItem item,
                                          final @NotNull Runnable beforeRun) {
        final var options = item.options();
        return dispatch(holder, options.clickHandler(), options.clickRequirements(), beforeRun)
                || dispatch(holder, options.leftClickHandler(), options.leftClickRequirements(), beforeRun)
                || dispatch(holder, options.rightClickHandler(), options.rightClickRequirements(), beforeRun)
                || dispatch(holder, options.middleClickHandler(), options.middleClickRequirements(), beforeRun)
                || dispatch(holder, options.shiftLeftClickHandler(), options.shiftLeftClickRequirements(), beforeRun)
                || dispatch(holder, options.shiftRightClickHandler(), options.shiftRightClickRequirements(), beforeRun);
    }

    public static boolean hasAnyHandler(final @NotNull MenuItem item) {
        final var options = item.options();
        return options.clickHandler().isPresent() || options.leftClickHandler().isPresent()
                || options.rightClickHandler().isPresent() || options.middleClickHandler().isPresent()
                || options.shiftLeftClickHandler().isPresent() || options.shiftRightClickHandler().isPresent();
    }
}
