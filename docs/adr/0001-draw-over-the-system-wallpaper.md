# Draw over the system wallpaper rather than painting our own background

Themes ship a wallpaper, so Less could simply render that image as an opaque
background and own its appearance completely. It doesn't: the launcher window is
transparent and the real system Wallpaper shows through, exactly as other
launchers do. An opaque background would break live wallpapers, kill the
system's wallpaper parallax on gestures, and leave Home looking unrelated to the
lock screen and recents. Picking a Theme applies that Theme's wallpaper through
`WallpaperManager` so Home, the lock screen, and recents stay the same picture.

## Consequences

Home wears a light Scrim and a text halo so the Wallpaper still reads while the
clock stays legible over busy paintings. Drawer and Settings sit behind a
stronger Scrim so their denser text stays readable regardless of what is behind
them.
