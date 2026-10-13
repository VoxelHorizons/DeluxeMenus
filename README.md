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

Before any configured player slot is changed, DeluxeMenus persists the original contents to `plugins/DeluxeMenus/player-slot-recovery/`. The original slots are restored when the menu closes, changes to another DeluxeMenus menu, the player disconnects, or the plugin shuts down. Recovery files are also restored after an interrupted server session before another player-slot UI is used.


## Optional Citizens NPC skin portraits (VoxelHorizons fork)

When [Citizens](https://www.citizensnpcs.co/) is installed, this fork registers an optional menu item material prefix `citizenshead-`. It creates a real player-head `ItemStack` whose texture comes from the selected Citizens NPC's **saved SkinTrait texture**. It does not use the viewer's skin or perform a network username lookup. Add `item_model` to render this head through your existing player-portrait model/shader.

Citizens' built-in NPC command trait already replaces **`<npc>` with the clicked NPC's numeric ID** (and `<p>` with the player name); DeluxeMenus just needs a declared `npc` argument to accept it. Attach this command **to an NPC**:

```text
/npc command add -p npcprofile <npc>
```

`-p` executes the command as the clicking player, who must have permission to run that menu's `open_command`. For NPC #42 the command sent to DeluxeMenus is `/npcprofile 42`.

Example menu in `plugins/DeluxeMenus/gui_menus/npcprofile.yml`:

```yaml
menu_title: '&8NPC Profile #{npc}'
open_command: npcprofile
register_command: true
args:
  - npc
size: 27
items:
  npc_portrait:
    material: 'citizenshead-{npc}'
    # Replace with the actual player-head portrait item model in your pack.
    item_model: 'voxel:ui/player_portrait'
    slot: 13
    display_name: '&eNPC #{npc}'
    lore:
      - '&7This uses the NPC skin, not the viewer skin.'
```

The argument `{npc}` is scoped to the menu instance. Multiple NPCs can execute the same menu command without mixing textures, and other items/actions can refer to `{npc}` (e.g. in commands or lore). Clicking NPC #15 displays NPC #15's current stored skin; another click with NPC #42 displays NPC #42. The skin cache is keyed by NPC ID **and the current texture property**; if Citizens changes the NPC's skin, the menu uses the new property on its next redraw. The cache is bounded to 256 head entries and cleared on DeluxeMenus reload.

**Optional behavior:** If Citizens is not installed, the hook is not registered, and DeluxeMenus behaves as before. If the NPC ID is invalid, removed, or its skin texture has not yet loaded, the hook returns an **untextured head** rather than unexpectedly using the viewer's skin. Reload/refresh the menu after Citizens has finished fetching or setting that NPC skin. Server-side Citizens NPC IDs refer to NPC registry records; they are **not** player UUIDs or usernames. A client-side shader must support rendering a properly textured player-head item for the portrait to display correctly.

This feature deliberately does not require the Citizens JAR as a compile or runtime dependency; Citizens is a `softdepend` and the API is accessed only when the plugin is enabled.
