package com.extendedclip.deluxemenus.menu.command;

import com.extendedclip.deluxemenus.DeluxeMenus;
import com.extendedclip.deluxemenus.menu.Menu;
import com.extendedclip.deluxemenus.utils.DebugLevel;
import com.extendedclip.deluxemenus.utils.StringUtils;
import me.clip.placeholderapi.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public class RegistrableMenuCommand extends Command {

    private static final String FALLBACK_PREFIX = "DeluxeMenus".toLowerCase(Locale.ROOT).trim();
    private static CommandMap commandMap = null;
    private static final Map<String, RegistrableMenuCommand> ROOTS = new HashMap<>();

    private final DeluxeMenus plugin;

    private Menu menu;
    private boolean registered = false;
    private boolean unregistered = false;

    public RegistrableMenuCommand(final @NotNull DeluxeMenus plugin,
                                  final @NotNull Menu menu) {
        super(menu.options().commands().isEmpty() ? menu.options().name() : root(menu.options().commands().get(0)));
        this.plugin = plugin;
        this.menu = menu;

        Set<String> roots = new LinkedHashSet<>();
        for (String pattern : menu.options().commands()) roots.add(root(pattern));
        roots.remove(getName());
        setAliases(new ArrayList<>(roots));
    }


    private static String root(String pattern) {
        return pattern.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
    }

    private static final class Match {
        final Menu menu;
        final Map<String, String> args;
        final int score;
        Match(Menu menu, Map<String, String> args, int score) {
            this.menu = menu;
            this.args = args;
            this.score = score;
        }
    }

    private static Match findMatch(Menu candidate, String label, String[] values) {
        Match best = null;
        for (String pattern : candidate.options().commands()) {
            String[] tokens = pattern.trim().split("\\s+");
            if (!tokens[0].equalsIgnoreCase(label)) continue;
            int count = tokens.length - 1;
            boolean self = count == 1 && tokens[1].equalsIgnoreCase("<player>") && values.length == 0;
            if (values.length < count && !self) continue;
            Map<String, String> args = new HashMap<>();
            int score = tokens.length == 1 && values.length == 0 ? 20 : 0;
            boolean valid = true;
            for (int i = 1; i < tokens.length; i++) {
                String token = tokens[i];
                if (token.matches("<[a-zA-Z][a-zA-Z0-9_]*>")) {
                    args.put(token.substring(1, token.length() - 1),
                            i <= values.length ? values[i - 1] : "");
                } else if (i > values.length || !token.equalsIgnoreCase(values[i - 1])) {
                    valid = false;
                    break;
                } else {
                    score += 10;
                }
            }
            if (!valid) continue;
            if (count == 0 && !candidate.options().arguments().isEmpty()) {
                if (values.length < candidate.options().arguments().size()) continue;
                for (int i = 0; i < candidate.options().arguments().size(); i++) {
                    String key = candidate.options().arguments().get(i);
                    args.put(key, i == candidate.options().arguments().size() - 1
                            ? String.join(" ", Arrays.asList(values).subList(i, values.length))
                            : values[i]);
                }
            } else if (values.length > count) continue;
            if (best == null || score > best.score) best = new Match(candidate, args, score);
        }
        return best;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] values) {
        if (unregistered) return true;
        if (!(sender instanceof Player)) {
            Msg.msg(sender, "Menus can only be opened by players!");
            return true;
        }
        return dispatch((Player) sender, label, values, true);
    }

    public static boolean dispatch(Player viewer, String label, String[] values, boolean registeredOnly) {
        Match best = null;
        for (Menu candidate : Menu.getAllMenus()) {
            if (candidate.options().registerCommands() != registeredOnly) continue;
            Match matched = findMatch(candidate, label, values);
            if (matched != null && (best == null || matched.score > best.score)) best = matched;
        }
        if (best == null) return false;
        String name = best.args.get("player");
        if (name != null && name.isEmpty()) {
            name = viewer.getName();
            best.args.put("player", name);
        }
        Player target = name == null ? null : Bukkit.getPlayerExact(name);
        best.menu.openMenu(viewer, best.args, target);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender,
                                              @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player) || args.length == 0) return Collections.emptyList();
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        Set<String> matches = new LinkedHashSet<>();
        for (Menu candidate : Menu.getAllMenus()) {
            if (!candidate.options().registerCommands()) continue;
            for (String pattern : candidate.options().commands()) {
                String[] tokens = pattern.trim().split("\\s+");
                if (!tokens[0].equalsIgnoreCase(alias) || args.length >= tokens.length) continue;
                boolean valid = true;
                for (int i = 1; i < args.length; i++) {
                    if (!tokens[i].matches("<[a-zA-Z][a-zA-Z0-9_]*>")
                            && !tokens[i].equalsIgnoreCase(args[i - 1])) valid = false;
                }
                if (!valid) continue;
                String token = tokens[args.length];
                if (token.equalsIgnoreCase("<player>")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (((Player)sender).canSee(player) && player.getName().toLowerCase(Locale.ROOT).startsWith(prefix))
                            matches.add(player.getName());
                    }
                } else if (!token.startsWith("<") && token.toLowerCase(Locale.ROOT).startsWith(prefix)) matches.add(token);
            }
        }
        List<String> result = new ArrayList<>(matches);
        result.sort(String.CASE_INSENSITIVE_ORDER);
        return result;
    }

    public void register() {
        if (registered) {
            throw new IllegalStateException("This command was already registered!");
        }
        if (registered) {
            throw new IllegalStateException("This command was already registered!");
        }

        registered = true;
        if (ROOTS.containsKey(getName().toLowerCase(Locale.ROOT))) return;

        if (commandMap == null) {
            try {
                final Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                f.setAccessible(true);
                commandMap = (CommandMap) f.get(Bukkit.getServer());
            } catch (final @NotNull Exception exception) {
                plugin.printStacktrace(
                        "Something went wrong while trying to register command: " + this.getName(),
                        exception
                );
                return;
            }
        }
        if (commandMap == null) {
            try {
                final Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                f.setAccessible(true);
                commandMap = (CommandMap) f.get(Bukkit.getServer());
            } catch (final @NotNull Exception exception) {
                plugin.printStacktrace(
                        "Something went wrong while trying to register command: " + this.getName(),
                        exception
                );
                return;
            }
        }

        boolean registered = commandMap.register(FALLBACK_PREFIX, this);
        if (registered) ROOTS.put(getName().toLowerCase(Locale.ROOT), this);
        if (registered) {
            plugin.debug(
                    DebugLevel.LOW,
                    Level.INFO,
                    "Registered command: " + this.getName() + " for menu: " + menu.options().name()
            );
        } else {
            plugin.debug(
                    DebugLevel.HIGHEST,
                    Level.WARNING,
                    "Failed to register command: " + this.getName() + " for menu: " + menu.options().name()
                            + ". A command with that name already exists. Use /deluxemenus:" + this.getName() + " instead."
            );
        }
    }

    public void unregister() {
        if (!registered) {
            throw new IllegalStateException("This command was not registered!");
        }

        if (unregistered) {
            throw new IllegalStateException("This command was already unregistered!");
        }

        unregistered = true;
        if (ROOTS.get(getName().toLowerCase(Locale.ROOT)) != this) {
            this.menu = null;
            return;
        }
        ROOTS.remove(getName().toLowerCase(Locale.ROOT));

        if (commandMap == null) {
            this.menu = null;
            return;
        }

        Field cMap;
        Field knownCommands;
        try {
            cMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cMap.setAccessible(true);
            knownCommands = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommands.setAccessible(true);

            final Map<String, Command> knownCommandsMap = (Map<String, Command>) knownCommands.get(cMap.get(Bukkit.getServer()));

            // We need to remove every single alias because CommandMap#register() adds them all to the map.
            // If we do not remove them, then we will have dangling references to the command.
            knownCommandsMap.remove(this.getName());
            knownCommandsMap.remove(FALLBACK_PREFIX + ":" + this.getName());

            for (String alias : this.getAliases()) {
                knownCommandsMap.remove(alias);
                knownCommandsMap.remove(FALLBACK_PREFIX + ":" + alias);
            }

            boolean unregistered = this.unregister((CommandMap) cMap.get(Bukkit.getServer()));
            this.unregister(commandMap);
            if (unregistered) {
                plugin.debug(
                        DebugLevel.HIGH,
                        Level.INFO,
                        "Successfully unregistered command: " + this.getName()
                );
            } else {
                plugin.debug(
                        DebugLevel.HIGHEST,
                        Level.WARNING,
                        "Failed to unregister command: " + this.getName()
                );
            }
        } catch (final @NotNull Exception exception) {
            plugin.printStacktrace(
                    "Something went wrong while trying to unregister command: " + this.getName(),
                    exception
            );
        }

        this.menu = null;
    }

    public boolean registered() {
        return registered;
    }
}
