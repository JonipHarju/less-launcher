# Less

A minimal Android launcher. The phone shows a clock, a date, and a short list of
apps you chose. Everything else is one swipe away.

## Language

### Surfaces

**Home**:
The launcher's primary surface — clock, date, and Favorites over the wallpaper.
There is exactly one; it does not paginate.
_Avoid_: home screen page, desktop, main screen

**Drawer**:
The full list of installed apps, opened from Home and dismissed back to it.
Carries the search field and the entry point to Settings.
_Avoid_: app list, menu, all-apps, app tray

**Setup**:
The three screens a new user meets before Home, in order: the Theme picker, the
request to become the default launcher, and the Favorites picker. It runs once.
The Theme comes first deliberately — Less shows what it is before it asks the
user for anything.
_Avoid_: onboarding, welcome screen, wizard, first-run flow

**Settings**:
The one screen holding every option, reached from the gear in the Drawer's top
bar and dismissed back to it. There is exactly one; options are never scattered
across surfaces.
_Avoid_: preferences, options screen, config

### Behaviour

**Drawer Open Direction**:
The swipe on Home that opens the Drawer — up or down — and, inverted, the swipe
that closes it. Stored, not hard-coded, so it can change without restructuring
the surfaces.
_Avoid_: swipe gesture, open gesture

**Home Role**:
The platform's own name for being the default launcher: what Android hands to
the app that opens when the user presses home. Less asks for it with the
platform role request, and with the home-app settings screen on a device that
does not honour one. Until Less holds it the Drawer carries a standing prompt;
once Less has held it the prompt is gone for good, because handing the role to
another launcher is a choice and not a mistake to correct.
_Avoid_: launcher permission, home permission, default-app setting

**Home Alignment**:
Whether Home lays its clock, date, and Favorites out from the left edge or
centred across the width. The user's choice, and the only thing that moves them.
_Avoid_: gravity, text alignment, layout mode

**Soft Cap**:
The eight Favorites past which Home stops being a short list. It advises and
never blocks: the ninth Favorite is pinned, the user is told, and Home scrolls.
_Avoid_: limit, maximum, quota

**Configuration**:
Everything the user chose — Favorites with their order and custom labels, Hidden
Apps, the settings and the Theme those settings name. Android's automatic backup
carries it to a restored phone, and the user can additionally write it to a file
of their own and read it back. What the device answers for itself stays with the
device: how far Setup got, and whether Less has held the Home Role, are left out
of the exported file and cleared again after a restore.
_Avoid_: backup, profile, preset, user data, settings file

### Apps

**Favorite**:
An app the user has placed on Home, with its own position and optional custom
label. Favorites are chosen deliberately; nothing promotes an app to one.
_Avoid_: pinned app, shortcut, dock item

**Everyday Intent**:
One of the plain things a phone does — dialling, messaging, taking a photo,
opening a web page — named as an intent so that the platform, not Less, decides
which app answers it. Setup proposes the answers as the first Favorites, so a
fresh Home works on any device without Less naming a single package.
_Avoid_: default app, stock app, system app

**Hidden App**:
An installed app the user has excluded from the Drawer. It remains installed and
launchable, just not listed.
_Avoid_: blocked app, disabled app, archived app

**Tombstone**:
A Favorite whose app has become temporarily unavailable — a work profile turned
off, storage unmounted — shown in place and dismissible. An app the user
uninstalls leaves no Tombstone, because that removal was intentional; a
Tombstone marks a loss the user did not ask for.
_Avoid_: placeholder, ghost, orphan

### Appearance

**Theme**:
A named, fixed preset describing how Home and the Drawer look: a wallpaper and
its Credit, a font, text and accent colours, a Scrim, an Icon Mode, and a Drawer
Treatment. A Theme changes appearance only — never behaviour, never which
features exist. Themes are authored, not user-editable.
_Avoid_: skin, style, palette, colour scheme

**Wallpaper**:
The Android system wallpaper, owned by the OS and shared with the lock screen
and recents. Less draws over it and replaces it when the user picks a Theme.
_Avoid_: background, backdrop

**Scrim**:
The gradient a Theme lays over the Wallpaper so text stays legible. Home wears a
light one so the Wallpaper still reads; Drawer and Settings wear a stronger one.
_Avoid_: overlay, dim, shade, tint

**Icon Mode**:
Whether a Theme shows app icons in their original colours, tinted to a single
theme colour, or not at all. Declared per Theme; the user can override it
globally.
_Avoid_: icon style, icon pack, icon theme

**Themeable Layer**:
The monochrome layer an app may supply alongside its icon, meant to be recoloured
by the launcher. Not every app ships one. Tinted Icon Mode uses it where it
exists and falls back to the Desaturated Tint where it doesn't.
_Avoid_: mono icon, monochrome icon, themed icon

**Desaturated Tint**:
What Tinted Icon Mode does to an app that supplies no Themeable Layer: strip the
colour from its own artwork and scale what is left by the Theme colour, so the
icon keeps its light and dark detail. Never a derived silhouette — see ADR-0005.
_Avoid_: greyscale, silhouette, fallback icon

**Drawer Treatment**:
How a Theme renders the Drawer's background against the Wallpaper — blurred,
flat translucent, or opaque.
_Avoid_: drawer style, drawer background

**Credit**:
The artist, title, year, collection, and source recorded for a Theme's
wallpaper, shown to the user in the Theme picker. Every bundled wallpaper has
one.
_Avoid_: attribution, licence, source
