# Dynamic inventory titles and `[refresh]`

DeluxeMenus now evaluates `menu_title` again whenever the currently displayed
menu is refreshed, including explicit `[refresh]` actions and periodic refreshes.

```yaml
menu_title: '%logic_vhshop-title%'
size: 54
open_command:
  - servershop
items:
  plus:
    material: GLASS_PANE
    slot: 47
    display_name: '&aAdd one'
    left_click_commands:
      - '[meta] add vhcartsmall INTEGER 1'
      - '[refresh]'
```

`%logic_vhshop-title%` can return complete font-aware titles built with
`<font:voxel:row_6>`, placeholders, glyphs and offsets. On refresh the
title is re-evaluated with the same player and argument context as on open.

If the resulting component is unchanged, no title packet is sent.
If it changed, a new title is sent **for the same container ID**, and
the existing slots are re-synchronised without reopening the inventory.
Consequently `open_commands`, `close_commands`, and protected player
inventory snapshots are not disturbed.

This uses Paper's current native window-title packet path to preserve
Adventure font styling. It does not require ProtocolLib or PacketEvents.
If the server version cannot support it, title changes are skipped with
one warning, while normal item refresh continues.

For real-money carts, inventory titles and `[meta]` values are display
state only. Checkout must independently validate package quantities
and compute the payable total server-side.
