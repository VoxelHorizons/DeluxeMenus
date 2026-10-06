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

When a menu has at least one active `player_slot` item, DeluxeMenus persists the **complete player inventory** to `plugins/DeluxeMenus/player-inventory-recovery/` and temporarily clears it. This prevents normal inventory items from overlapping UI items. The exact storage/hotbar contents, armor/offhand contents, and selected hotbar slot are restored when the player-slot UI closes. During `[openguimenu]` transitions, the snapshot is retained when the destination also uses `player_slot`; a destination menu with no `player_slot` restores the inventory before opening. Recovery files are also restored after an interrupted server session.
