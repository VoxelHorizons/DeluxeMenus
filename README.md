[logo]: https://github.com/HelpChat/DeluxeMenus/assets/52609756/f24ac57d-98db-4d57-a723-791a2654e73f

[issues]: https://github.com/HelpChat/DeluxeMenus/issues
[licenseImg]: https://img.shields.io/github/license/helpchat/deluxemenus?&logo=github
[license]: https://github.com/HelpChat/DeluxeMenus/blob/master/LICENSE

[bstatsImg]: https://img.shields.io/bstats/servers/445
[bstats]: https://bstats.org/plugin/bukkit/DeluxeMenus/445

[discordImg]: https://img.shields.io/discord/164280494874165248?color=5562e9&logo=discord&logoColor=white
[discord]: https://helpch.at/discord
[spigot]: https://www.spigotmc.org/resources/11734/

[ci]: http://ci.extendedclip.com/job/DeluxeMenus/
[ciImg]: http://ci.extendedclip.com/buildStatus/icon?job=DeluxeMenus

[contributing]: https://github.com/HelpChat/DeluxeMenus/blob/main/CONTRIBUTING.md

[![logo]][spigot]

[![ciImg]][ci] [![bstatsImg]][bstats] [![discordImg]][discord] [![licenseImg]][license] [![GitBook](https://img.shields.io/static/v1?message=Documented%20on%20GitBook&logo=gitbook&logoColor=ffffff&label=%20&labelColor=5c5c5c&color=3F89A1)](https://wiki.helpch.at/helpchat-plugins/deluxemenus)


# Information
[DeluxeMenus][spigot] is the all in one inventory GUI menu plugin!
You can create GUI menus that open with custom commands that will show stats or perform actions specific to the player who opened it. Your menus are fully configurable. You can create menus that show specific items to different players, or perform different actions depending on what javascript requirement they have for the specific slot in a certain GUI.

DeluxeMenus depends on [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/).

## Contribute
If you would like to contribute towards DeluxeMenus should you take a look at our [Contributing file][contributing] for the ins and outs on how you can do that and what you need to keep in mind.

## Support
- [Issue Tracker][issues]
- [Discord Support][discord]

## Quick Links
- [Wiki](https://wiki.helpch.at/clips-plugins/deluxemenus/)
- [CI Server][ci]
- [Spigot Page][spigot]
- [Plugin Statistics][bstats]



## Player inventory UI slots

This fork supports rendering a menu item in the player's visible inventory area while a DeluxeMenus menu is open.

```yaml
items:
  help:
    material: PAPER
    item_model: voxel:ui/help
    slot: 53
    player_slot: 8
    display_name: '&eHelp'
    lore:
      - '&7Click for help.'
    left_click_commands:
      - '[player] help'
```

`slot` remains the DeluxeMenus logical slot used for priorities/view requirements. When `player_slot` is present, the item is rendered in that player inventory slot instead of the top menu inventory. Valid player slots are `0-35`: hotbar `0-8`, then the main player inventory `9-35`.

### Bedrock (Geyser/Floodgate) form frontend

Java chest menus that rely on bitmap-font titles, negative-space glyphs or `item_model`
overlays cannot render on Bedrock. When Floodgate (preferred, works behind a proxy) or a
local Geyser-Spigot is enabled, **Bedrock players receive a native form instead of the
chest GUI**; Java players are unaffected.

- The form is built from the same menu holder, so open requirements, argument
  requirements, `/dm open <menu> <args>` arguments, `p:<player>` placeholder targets and
  view requirements apply exactly as for Java.
- Every item with any click handler becomes a button (sorted by slot). Pressing it runs the
  item's `click_commands`, falling back to left, right, middle, shift-left, shift-right, behind
  the matching click requirements and deny commands.
- An item without `click_commands` but with several distinct click handlers (e.g. left =
  buy one, shift-left = buy a stack) opens a second form listing those click types, so no
  Java action is lost; each runs behind its own click requirements.
- Items without click handlers (e.g. profile statistics) are shown as readable text above
  the buttons, including their full lore.
- Colour codes are kept; private-use glyphs (U+E000–U+F8FF and supplementary private use)
  and hex colours are removed. Unnamed items use their material name.
- After a press the form is shown again with the refreshed menu state, unless the actions
  closed the menu or opened another one. Closing the form closes the menu.
- Player inventories are never hidden or overwritten for Bedrock viewers; `player_slot`
  items appear as buttons instead.

- `bedrock_image` on an item adds a button image: an `https://` URL (placeholders and
  arguments are applied, e.g. `https://mc-heads.net/avatar/%player_name%` for a profile
  portrait) or a relative resource-pack texture path such as `textures/ui/voxel/shop`
  (no `..`, absolute paths or schemes).

Disable globally with `bedrock_forms: false` in `config.yml`, or per menu with
`bedrock_form: false` at the menu root (Bedrock players then get the Java chest menu).

### Fullscreen inventory protection

Fullscreen resource-pack interfaces should hide ordinary inventory items (including the hotbar, armor, and offhand) while the GUI is visible. Add `hide_player_inventory: true` to the **menu root**, alongside `menu_title` and `size`:

```yaml
menu_title: ':testing_network_background:'
size: 54
hide_player_inventory: true
items:
  help:
    material: PAPER
    item_model: voxel:ui/help
    slot: 53
    player_slot: 8
    display_name: '&eHelp'
```

This option defaults to `false` on ordinary menus. It is **automatically enabled** when an active menu item uses `player_slot`, even when the option is omitted. In fullscreen mode the plugin stores the full player inventory (36 storage/hotbar slots, armor, offhand/extra slots, carried cursor stack, and selected hotbar slot), hides all those items, then draws only the menu's own `player_slot` elements. Original items are restored on close, menu transition, logout, reload, and shutdown. Unlike the old per-slot implementation, normal hotbar items cannot overlap the fullscreen overlay.

Snapshots are written to `plugins/DeluxeMenus/player-slot-recovery/<uuid>.yml` **before** inventory contents are cleared. Existing legacy per-slot recovery files continue to be recognized. If a recovery file is unreadable or cannot be restored, the plugin refuses to overwrite it and logs a server-side error. Do not manually delete recovery files while users are waiting for their original inventory to be restored.

**Important:** Fullscreen inventory protection is designed for visual/menu interactions. Commands that read or alter a player's *real* inventory while it is hidden (e.g. shop purchases, item requirements, item rewards) must account for the temporarily empty inventory. Test economy/shop actions separately before enabling this mode on transaction menus.

### Manual verification

- Put a distinctive item in hotbar slot 0, other storage slots, armor, and offhand; open the fullscreen GUI and confirm none show through the overlay.
- Close with Escape and confirm the exact original inventory and selected hotbar slot return.
- Switch between two protected menus, then from a protected menu to an ordinary menu; check that nothing is lost or duplicated.
- Verify `player_slot` buttons remain clickable after periodic menu refreshes and after a view requirement changes.
- Test player logout/rejoin and a normal server restart with a protected GUI open; verify recovery and removal of the recovery file.



## Optional Citizens NPC skin portraits (VoxelHorizons fork)

Citizens already expands `<p>` into the clicking player's username and `<npc>` into the NPC's numeric ID. The integration in this fork uses that ID to create an ordinary textured player-head item from the NPC's **stored SkinTrait texture**. Add your existing player-portrait shader's `item_model` to render this head with the NPC's skin.

### Recommended: NPC-only menu (no public open command)

Create `plugins/DeluxeMenus/gui_menus/npcprofile.yml`. **Do not add `open_command` or `register_command: true`**. The menu remains available by its file/menu name to the privileged `/dm open` command, but players cannot run a registered `/npcprofile` command:

```yaml
menu_title: '&8NPC Profile #{npc}'
size: 27
args:
  - npc
items:
  npc_portrait:
    material: 'citizenshead-{npc}'
    # Replace with the existing portrait model from your resource pack.
    item_model: 'voxel:ui/player_portrait'
    slot: 13
    display_name: '&eNPC #{npc}'
    lore:
      - '&7This is the NPC skin, not your player skin.'
```

Select an NPC, then add this Citizens click command **without `-p` or `-o`**:

```text
/npc command add dm open npcprofile <p> <npc>
```

Citizens defaults to executing the command **as the server**, giving it permission for the restricted admin-only `/dm open`. For NPC #42, clicked by `FlopsiNZ`, Citizens invokes `dm open npcprofile FlopsiNZ 42`. The DeluxeMenus admin open command now forwards `42` to the declared `npc` menu argument. The GUI and any included click actions can reference `{npc}`, and the portrait material `citizenshead-{npc}` reads NPC #42's **actual texture**. No global last-selected NPC, viewer-skin substitution or Citizens API hard dependency.

### Admin open argument syntax

```text
/dm open <menu> [viewer] [-p:placeholder-player] [menu-argument-1] [menu-argument-2] ...
```

For a menu with `args: [npc]`, the ID goes **after** the player name. Named menu arguments follow the same order as registered `open_command` arguments, with the last declared argument consuming any remaining words. Existing `/dm open <menu>`, `/dm open <menu> <viewer>`, `/dm open <menu> -p:<player>`, and `/dm open <menu> <viewer> -p:<player>` continue to work. The privileged command retains `deluxemenus.open` and `deluxemenus.open.others` permissions for player senders; the `-p:` override retains its own permission/exemption checks.

**Avoid Citizens `-p`:** That would execute `/dm open` as the clicking player and require admin command permissions. The default console sender avoids granting everyone `deluxemenus.open` or temporary OP. You can add `--permission your.permission` to the Citizens command if only certain players should open this NPC menu.

### Optional dependency and skin lifecycle

The `citizenshead-` material prefix is registered only if Citizens is enabled (`softdepend: [Citizens]`). It reads the NPC's saved base64 skin texture without online username lookups. The cache is keyed by NPC ID and the current texture value (bounded to 256 entries, cleared on menu reload); a changed skin appears on the next redraw. If an ID is invalid or the NPC skin is not yet available, the portrait item is an untextured head rather than somebody else's skin. Reopen/refresh once Citizens finishes loading the skin. Citizens NPC IDs are registry identifiers, not usernames or skin UUIDs.

If you explicitly *want* a public command instead, you can still configure `open_command: npcprofile` plus `register_command: true` and use Citizens `/npc command add -p npcprofile <npc>`, but that exposes the menu command to players according to its permissions.

### Custom fonts in titles and item text

This fork accepts `<font:namespace:path>text</font>` in menu titles, item
names and lore (and other text rendered with the shared component parser).
Use a fully qualified, lowercase resource-pack font ID. For example,
`assets/voxel/font/shop.json` is selected as `voxel:shop`:

```yaml
menu_title: ':offset_-8::server_shop_background::offset_-70:<font:voxel:shop>&fTokens: 100</font>'
```

The second offset is an example: it moves the cursor relative to the end of
the background glyph and should be adjusted for your overlay. Keep background
and offset glyphs outside the font tag. Vertical alignment is controlled by
the font's bitmap `ascent` values; the font tag itself does not add padding.
Players must have the resource pack containing the selected font.

Legacy `&` colours, hex colours, section-symbol codes and placeholders continue
to work. Colour/decoration state continues across font tags; `&r` resets legacy
formatting but does not close a font scope. Font tags may nest; closing a tag
restores its enclosing font, or `minecraft:default` outside all scopes. Unbalanced
recognised font tags leave the text literal. This is font-tag support only, not
a general MiniMessage parser; other tags remain literal.
