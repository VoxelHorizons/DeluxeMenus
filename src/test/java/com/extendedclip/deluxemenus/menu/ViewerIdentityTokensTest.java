package com.extendedclip.deluxemenus.menu;

import com.extendedclip.deluxemenus.utils.StringUtils;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ViewerIdentityTokensTest {

    @Test
    void viewerIdentityDoesNotChangeWhenPlaceholderTargetIsAnotherPlayer() {
        UUID viewerUuid = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
        UUID targetUuid = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
        Player viewer = player("Viewer", viewerUuid);
        Player target = player("Target", targetUuid);

        MenuHolder holder = new MenuHolder(null, viewer);
        holder.setPlaceholderPlayer(target);
        holder.setTypedArgs(Map.of("player", "Target", "target_uuid", targetUuid.toString()));
        holder.parsePlaceholdersAfterArguments(true);

        assertEquals(viewerUuid.toString(), holder.setArguments("{viewer_uuid}"));
        assertEquals("Viewer", holder.setArguments("{viewer_name}"));
        assertEquals("Target", holder.setArguments("{player}"));
        assertEquals("{Target}", holder.setArguments("{{player}}"));
        assertNotEquals(targetUuid.toString(), holder.setArguments("{viewer_uuid}"));

        // The production requirement uses !string equals. Verify its resolved
        // operands here without invoking a live PlaceholderAPI plugin in CI.
        assertTrue(!holder.setArguments("{viewer_uuid}").equals(holder.setArguments("{target_uuid}")));

        holder.setTypedArgs(Map.of("player", "Viewer", "target_uuid", viewerUuid.toString()));
        assertFalse(!holder.setArguments("{viewer_uuid}").equals(holder.setArguments("{target_uuid}")));
    }

    @Test
    void viewerTokensAreAvailableBeforeExecutingActions() {
        Player viewer = player("Viewer", UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"));
        String action = "Viewer={viewer_name} UUID={viewer_uuid} Target={player}";

        assertEquals(
                "Viewer=Viewer UUID=aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa Target={player}",
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
