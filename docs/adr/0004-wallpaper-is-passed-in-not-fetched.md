# The wallpaper is passed into the themed surface, not read from the system

The composable that renders a Theme receives the wallpaper as a parameter rather
than reading `WallpaperManager` itself. In the app, the system wallpaper is
passed in; in tests, a fixed bitmap is. This exists so the six Themes can be
covered by screenshot tests — which run without a device and so have no system
wallpaper — and those tests are the only thing that will catch a Theme becoming
unreadable. Inlining the `WallpaperManager` call would look tidier and would
make the Theme layer untestable.
