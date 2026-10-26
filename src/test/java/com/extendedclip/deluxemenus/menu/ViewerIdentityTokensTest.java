package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.utils.StringUtils;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ViewerIdentityTokensTest {

    @Test
    void viewerIdentityDoesNotChangeWhenPlaceholderTargetIsAnotherPlayer() {
        UUID viewerUuid = UUID.fromString("5727f056-bbe5-492d-bfa7-4a027f04d000");
        UUID targetUuid = UUID.fromString("00000000-0000-0000-0009-01f8c9264c8c");
        Player viewer = player("Viewer", viewerUuid);
        Player target = player("Target", targetUuid);

        MenuHolder holder = new MenuHolder(null, viewer);
        holder.setPlaceholderPlayer(target);
        holder.setTypedArgs(Map.of("player", "Target"));
        holder.parsePlaceholdersAfterArguments(true);

        assertEquals(viewerUuid.toString(), holder.setArguments("{viewer_uuid}"));
        assertEquals("Viewer", holder.setArguments("{viewer_name}"));
        assertEquals("Target", holder.setArguments("{player}"));
        assertEquals("{Target}", holder.setArguments("{{player}}"));
        assertNotEquals(targetUuid.toString(), holder.setArguments("{viewer_uuid}"));
    }

    @Test
    void viewerTokensAreAvailableBeforeExecutingActions() {
        Player viewer = player("Viewer", UUID.fromString("5727f056-bbe5-492d-bfa7-4a027f04d000"));
        String action = "Viewer={viewer_name} UUID={viewer_uuid} Target={player}";

        assertEquals(
                "Viewer=Viewer UUID=5727f056-bbe5-492d-bfa7-4a027f04d000 Target={player}",
                StringUtils.replaceViewerTokens(action, viewer)
        );
    }

    private static Player player(String name, UUID uuid) {
        return (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getName" -> name;
                    case "getUniqueId" -> uuid;
                    case "toString" -> name;
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
