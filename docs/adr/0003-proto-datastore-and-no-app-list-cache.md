# Proto DataStore for user data, and no cache of the installed-app list

Favorites, settings, and Hidden Apps live in Proto DataStore — a few dozen typed
records, no schema migrations worth a database. The installed-app list is *not*
persisted at all: it is queried live from `LauncherApps` into an in-memory
`StateFlow` refreshed by package broadcasts.

Caching the app list in Room would save roughly 100ms on a cold start that
rarely happens, in exchange for owning cache invalidation across installs,
uninstalls, updates, locale changes, and work-profile toggles — a permanent
correctness liability bought with a barely perceptible win. If a future reader
is tempted to add that cache, this is why it isn't there.
