package com.extendedclip.deluxemenus.command.subcommand;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.menu.Menu;
import com.extendedclip.deluxemenus.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OpenCommand extends SubCommand {

    private static final String OPEN_COMMAND = "deluxemenus.open";

    public OpenCommand(final @NotNull DeluxeMenus plugin) {
        super(plugin);
    }

    @Override
    public @NotNull String getName() {
        return "open";
    }

    @Override
    public void execute(final @NotNull CommandSender sender, final @NotNull List<String> arguments) {
        if (!sender.hasPermission(OPEN_COMMAND)) {
            plugin.sms(sender, Messages.NO_PERMISSION);
            return;
        }

        boolean player = (sender instanceof Player);

        if (arguments.isEmpty()) {
            plugin.sms(sender, Messages.WRONG_USAGE_OPEN_COMMAND);
            return;
        }

        if (Menu.getAllMenus().isEmpty()) {
            plugin.sms(sender, Messages.MENUS_LOADED.message().replaceText(AMOUNT_REPLACER_BUILDER.replacement("There are no").build()));
            return;
        }

        // Command arguments: /dm open <menu> [viewer] [-p:placeholder-player] [menu args...]
        // Citizens substitutes <p> and <npc> before executing its console command.
        // The existing registered open_command is not needed for this pathway.
        final OpenMenuArguments parsed;
        try {
            parsed = OpenMenuArguments.parse(arguments);
        } catch (IllegalArgumentException invalid) {
            sender.sendMessage("Invalid /dm open arguments: " + invalid.getMessage());
            return;
        }

        final String viewerName = parsed.viewerName();
        if (viewerName == null && !player) {
            plugin.sms(sender, Messages.MUST_SPECIFY_PLAYER);
            return;
        }
        if (viewerName != null && player && !sender.hasPermission("deluxemenus.open.others")) {
            plugin.sms(sender, Messages.NO_PERMISSION);
            return;
        }
        if (parsed.placeholderPlayerName() != null && !sender.hasPermission("deluxemenus.placeholdersfor")) {
            plugin.sms(sender, Messages.NO_PERMISSION_PLAYER_ARGUMENT);
            return;
        }

        Player viewer = viewerName != null ? Bukkit.getPlayerExact(viewerName) : (Player) sender;
        if (viewer == null) {
            plugin.sms(sender, Messages.PLAYER_IS_NOT_ONLINE.message()
                    .replaceText(PLAYER_REPLACER_BUILDER.replacement(viewerName).build()));
            return;
        }

        Player placeholder = null;
        if (parsed.placeholderPlayerName() != null) {
            placeholder = Bukkit.getPlayerExact(parsed.placeholderPlayerName());
            if (placeholder == null) {
                plugin.sms(sender, Messages.PLAYER_IS_NOT_ONLINE.message().replaceText(
                        PLAYER_REPLACER_BUILDER.replacement(parsed.placeholderPlayerName()).build()));
                return;
            }
            if (placeholder.hasPermission("deluxemenus.placeholdersfor.exempt")) {
                plugin.sms(sender, Messages.PLAYER_IS_EXEMPT.message().replaceText(
                        PLAYER_REPLACER_BUILDER.replacement(parsed.placeholderPlayerName()).build()));
                return;
            }
        }

        Optional<Menu> menu = Menu.getMenuByName(arguments.get(0));
        if (menu.isEmpty()) {
            plugin.sms(sender, Messages.INVALID_MENU.message()
                    .replaceText(MENU_REPLACER_BUILDER.replacement(arguments.get(0)).build()));
            return;
        }

        final java.util.Map<String, String> typedArgs;
        try {
            typedArgs = parsed.typedArguments(menu.get().options().arguments());
        } catch (IllegalArgumentException invalid) {
            sender.sendMessage("Cannot open menu '" + arguments.get(0) + "': " + invalid.getMessage());
            return;
        }

        menu.get().openMenu(viewer, typedArgs, placeholder);
    }

    @Override
    public @Nullable List<String> onTabComplete(final @NotNull CommandSender sender, final @NotNull List<String> arguments) {
        if (!sender.hasPermission(OPEN_COMMAND)) {
            return null;
        }

        if (arguments.isEmpty()) {
            return List.of(getName());
        }

        if (arguments.size() > 4) {
            return null;
        }

        if (arguments.size() == 1) {
            if (arguments.get(0).isEmpty()) {
                return List.of(getName());
            }

            final String firstArgument = arguments.get(0).toLowerCase();

            if (getName().startsWith(firstArgument)) {
                return List.of(getName());
            }

            return null;
        }

        final String firstArgument = arguments.get(0).toLowerCase();

        if (!getName().equals(firstArgument)) {
            return null;
        }

        final Collection<String> menuNames = Menu.getAllMenuNames();

        if (menuNames.isEmpty()) {
            return null;
        }

        if (arguments.size() == 2) {
            final String secondArgument = arguments.get(1).toLowerCase();

            if (secondArgument.isEmpty()) {
                return List.copyOf(menuNames);
            }

            return menuNames.stream()
                    .filter(menuName -> menuName.toLowerCase().startsWith(secondArgument))
                    .collect(Collectors.toList());
        }

        final List<String> onlinePlayerNames = Bukkit.getOnlinePlayers()
                .stream()
                .map(Player::getName)
                .collect(Collectors.toList());

        if (arguments.size() == 3) {
            final String thirdArgument = arguments.get(2).toLowerCase();

            if (thirdArgument.isEmpty()) {
                return Stream.concat(onlinePlayerNames.stream(), Stream.of("-p:")).collect(Collectors.toList());
            }

            if (!thirdArgument.startsWith("-")) {
                return onlinePlayerNames.stream()
                        .filter(playerName -> playerName.toLowerCase().startsWith(thirdArgument))
                        .collect(Collectors.toList());
            }

            return onlinePlayerNames.stream()
                    .map(playerName -> "-p:" + playerName)
                    .filter(playerName -> playerName.toLowerCase().startsWith(thirdArgument))
                    .collect(Collectors.toList());
        }

        if (arguments.size() == 4) {
            final String thirdArgument = arguments.get(2).toLowerCase();
            final String fourthArgument = arguments.get(3).toLowerCase();

            if (!thirdArgument.startsWith("-p:")) {
                return null;
            }

            if (fourthArgument.isEmpty()) {
                return onlinePlayerNames;
            }

            return onlinePlayerNames.stream()
                    .filter(playerName -> playerName.toLowerCase().startsWith(fourthArgument))
                    .collect(Collectors.toList());
        }

        return null;
    }
}
