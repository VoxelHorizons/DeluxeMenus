package com.extendedclip.deluxemenus.hooks;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class CitizensSkinLookupTest {
    public static class SampleTrait {
        private final String texture;
        SampleTrait(String texture) { this.texture = texture; }
        public String getTexture() { return texture; }
    }

    private static String encoded(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test void parsesOnlyNumericNpcIds() {
        assertEquals(42, CitizensSkinLookup.parseNpcId("42"));
        assertEquals(0, CitizensSkinLookup.parseNpcId("0"));
        assertEquals(Integer.MAX_VALUE, CitizensSkinLookup.parseNpcId("2147483647"));
        for (String invalid : new String[]{"", "-1", "+1", "1.5", "npc", " 12", "<npc>",
                "99999999999", "2147483648"}) {
            assertThrows(IllegalArgumentException.class, () -> CitizensSkinLookup.parseNpcId(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> CitizensSkinLookup.parseNpcId(null));
    }

    @Test void extractsOriginalBase64TextureWithoutAlteringNpcSkin() {
        String raw = encoded("{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/sample\"}}}");
        assertEquals(raw, CitizensSkinLookup.textureFromTrait(new SampleTrait(raw)));
    }

    @Test void skipsUnknownUninitializedOrCorruptTextures() {
        assertNull(CitizensSkinLookup.textureFromTrait(null));
        assertNull(CitizensSkinLookup.textureFromTrait(new SampleTrait(null)));
        assertNull(CitizensSkinLookup.textureFromTrait(new SampleTrait("")));
        assertNull(CitizensSkinLookup.textureFromTrait(new SampleTrait("not base64")));
        assertNull(CitizensSkinLookup.textureFromTrait(new SampleTrait(encoded("{\"textures\":{}}"))));
    }

    @Test void missingCitizensPluginDoesNotRequireItsApi() {
        // Calls into optional library classes only after a lookup is requested.
        // Without Citizens on the test classpath the lookup returns null.
        assertNull(CitizensSkinLookup.textureForNpc(42));
    }
}
