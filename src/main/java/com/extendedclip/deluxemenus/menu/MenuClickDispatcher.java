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

    /** A Java click type an item reacts to, with its handler and requirements. */
    public record ClickOption(@NotNull String label, @NotNull Optional<ClickHandler> handler,
                              @NotNull Optional<RequirementList> requirements) {
    }

    /**
     * The distinct click actions a Bedrock player must be able to choose. Mirrors the Java
     * listener: a generic {@code click_commands} handler runs for every click type, so it is
     * the only option when present; otherwise each specific handler is offered.
     */
    public static @NotNull java.util.List<ClickOption> clickOptions(final @NotNull MenuItem item) {
        final var options = item.options();
        final java.util.List<ClickOption> out = new java.util.ArrayList<>();
        if (options.clickHandler().isPresent()) {
            out.add(new ClickOption("Select", options.clickHandler(), options.clickRequirements()));
            return out;
        }
        add(out, "Left click", options.leftClickHandler(), options.leftClickRequirements());
        add(out, "Right click", options.rightClickHandler(), options.rightClickRequirements());
        add(out, "Shift + left click", options.shiftLeftClickHandler(), options.shiftLeftClickRequirements());
        add(out, "Shift + right click", options.shiftRightClickHandler(), options.shiftRightClickRequirements());
        add(out, "Middle click", options.middleClickHandler(), options.middleClickRequirements());
        return out;
    }

    private static void add(final java.util.List<ClickOption> out, final String label,
                            final Optional<ClickHandler> handler, final Optional<RequirementList> requirements) {
        if (handler.isPresent()) out.add(new ClickOption(label, handler, requirements));
    }

    public static boolean hasAnyHandler(final @NotNull MenuItem item) {
        final var options = item.options();
        return options.clickHandler().isPresent() || options.leftClickHandler().isPresent()
                || options.rightClickHandler().isPresent() || options.middleClickHandler().isPresent()
                || options.shiftLeftClickHandler().isPresent() || options.shiftRightClickHandler().isPresent();
    }
}
