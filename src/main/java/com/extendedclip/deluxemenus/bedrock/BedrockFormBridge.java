package com.extendedclip.deluxemenus.bedrock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.logging.Level;

/**
 * Sends Cumulus simple forms through Floodgate (preferred, also works behind a proxy)
 * or a local Geyser-Spigot, by reflection through that plugin's class loader. No
 * compile-time or hard runtime dependency: without either plugin nobody is Bedrock.
 * API: GeyserApiBase#isBedrockPlayer/sendForm(UUID, Form), FloodgateApi#isFloodgatePlayer/
 * sendForm(UUID, Form), Cumulus SimpleForm.builder()…validResultHandler/closedOrInvalidResultHandler.
 */
public final class BedrockFormBridge {
    /** One form button; {@code imageType} is "URL", "PATH" or null. */
    public record Button(@NotNull String text, @Nullable String imageType, @Nullable String image) {
    }

    private final Plugin plugin;
    private final Object api;
    private final Method isBedrock;
    private final Method sendForm;
    private final ClassLoader loader;

    private BedrockFormBridge(Plugin plugin, Object api, Method isBedrock, Method sendForm, ClassLoader loader) {
        this.plugin = plugin;
        this.api = api;
        this.isBedrock = isBedrock;
        this.sendForm = sendForm;
        this.loader = loader;
    }

    /** @return a bridge, or null when neither Floodgate nor Geyser is enabled. */
    public static @Nullable BedrockFormBridge detect(final @NotNull Plugin plugin) {
        final BedrockFormBridge floodgate = attempt(plugin, "floodgate",
                "org.geysermc.floodgate.api.FloodgateApi", "getInstance",
                "org.geysermc.floodgate.api.FloodgateApi", "isFloodgatePlayer");
        if (floodgate != null) return floodgate;
        return attempt(plugin, "Geyser-Spigot", "org.geysermc.api.Geyser", "api",
                "org.geysermc.api.GeyserApiBase", "isBedrockPlayer");
    }

    private static @Nullable BedrockFormBridge attempt(Plugin plugin, String pluginName, String apiClass,
                                                       String accessor, String apiInterface, String bedrockCheck) {
        final Plugin provider = Bukkit.getPluginManager().getPlugin(pluginName);
        if (provider == null || !provider.isEnabled()) return null;
        try {
            final ClassLoader loader = provider.getClass().getClassLoader();
            final Object api = Class.forName(apiClass, true, loader).getMethod(accessor).invoke(null);
            if (api == null) return null;
            final Class<?> formClass = Class.forName("org.geysermc.cumulus.form.Form", true, loader);
            // Resolve on the public API interface: implementation classes may be non-public.
            final Class<?> apiType = Class.forName(apiInterface, true, loader);
            final Method isBedrock = apiType.getMethod(bedrockCheck, UUID.class);
            final Method sendForm = apiType.getMethod("sendForm", UUID.class, formClass);
            plugin.getLogger().info("Bedrock menu forms available through " + pluginName + ".");
            return new BedrockFormBridge(plugin, api, isBedrock, sendForm, loader);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException unavailable) {
            plugin.getLogger().log(Level.WARNING, "Bedrock menu forms unavailable through " + pluginName
                    + ": " + unavailable, unavailable instanceof RuntimeException ? unavailable : null);
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object imageType(final Class<?> type, final String name) {
        return Enum.valueOf((Class) type, name);
    }

    public boolean isBedrock(final @NotNull Player player) {
        try {
            return Boolean.TRUE.equals(isBedrock.invoke(api, player.getUniqueId()));
        } catch (ReflectiveOperationException | RuntimeException failure) {
            return false;
        }
    }

    /**
     * Callbacks run on the server main thread.
     *
     * @return false when the form could not be built or sent
     */
    public boolean send(final @NotNull Player player, final @NotNull String title, final @NotNull String content,
                        final @NotNull List<Button> buttons, final @NotNull IntConsumer onButton,
                        final @NotNull Runnable onClosed) {
        try {
            final Class<?> simpleForm = Class.forName("org.geysermc.cumulus.form.SimpleForm", true, loader);
            final Object builder = simpleForm.getMethod("builder").invoke(null);
            final Class<?> builderType = Class.forName("org.geysermc.cumulus.form.SimpleForm$Builder", true, loader);
            final Method clickedButtonId = Class.forName("org.geysermc.cumulus.response.SimpleFormResponse", true, loader)
                    .getMethod("clickedButtonId");
            builderType.getMethod("title", String.class).invoke(builder, title);
            builderType.getMethod("content", String.class).invoke(builder, content);
            final Method button = builderType.getMethod("button", String.class);
            final Class<?> imageTypeClass = Class.forName("org.geysermc.cumulus.util.FormImage$Type", true, loader);
            final Method imageButton = builderType.getMethod("button", String.class, imageTypeClass, String.class);
            for (final Button entry : buttons) {
                if (entry.imageType() != null && entry.image() != null) {
                    imageButton.invoke(builder, entry.text(), imageType(imageTypeClass, entry.imageType()), entry.image());
                } else {
                    button.invoke(builder, entry.text());
                }
            }
            final Consumer<Object> valid = response -> {
                final int id;
                try {
                    id = ((Number) clickedButtonId.invoke(response)).intValue();
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    plugin.getLogger().log(Level.WARNING, "Unreadable Bedrock form response", failure);
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> onButton.accept(id));
            };
            final Runnable closed = () -> Bukkit.getScheduler().runTask(plugin, onClosed);
            builderType.getMethod("validResultHandler", Consumer.class).invoke(builder, valid);
            builderType.getMethod("closedOrInvalidResultHandler", Runnable.class).invoke(builder, closed);
            final Object form = builderType.getMethod("build").invoke(builder);
            return Boolean.TRUE.equals(sendForm.invoke(api, player.getUniqueId(), form));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException failure) {
            plugin.getLogger().log(Level.WARNING, "Could not send Bedrock menu form to " + player.getName(), failure);
            return false;
        }
    }
}
