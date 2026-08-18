# Draw over the system wallpaper rather than painting our own background

Themes ship a wallpaper, so Less could simply render that image as an opaque
background and own its appearance completely. It doesn't: the launcher window is
transparent and the real system Wallpaper shows through, exactly as other
launchers do. An opaque background would break live wallpapers, kill the
system's wallpaper parallax on gestures, and leave Home looking unrelated to the
lock screen and recents. Picking a Theme therefore *offers* to apply its
wallpaper via `WallpaperManager` behind an explicit tap — a launcher should not
silently mutate a system-wide setting.

## Consequences

A Theme's text colours can end up over a wallpaper it was never designed for, so
every Theme carries a Scrim strong enough to guarantee legibility on its own,
independent of what is behind it.
