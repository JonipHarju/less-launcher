# Less has an icon in whatever launcher is currently the home app

`MainActivity` declares `CATEGORY_LAUNCHER` alongside `CATEGORY_HOME`, so Less
appears in the app list of whichever launcher is presently the home app, and
tapping it opens Less — first run included.

`CATEGORY_HOME` alone is enough to make Less selectable on the default-apps
screen, and that is all the manifest declared at first. It is not enough to make
Less findable. A user who installs a launcher and never opens Android's
default-apps screen has, with `HOME` only, no way to open the app they just
installed: it is in no drawer, on no home screen, and in no search result. The
one entry point is a settings page most people never visit.

## Considered Options

**`HOME` only, as shipped.** Purist and defensible — Less *is* the home screen,
not an app you launch, and once it holds the role the home button is the entry
point. It fails the person who has not granted the role yet, which is everyone
at least once.

**A separate settings activity carrying `LAUNCHER`,** as Nova and Niagara do.
The drawer entry opens the launcher's settings rather than the launcher. It is
the conventional shape, and it costs an activity whose only job is to route to a
surface `MainActivity` already owns. Rejected as machinery: what a user tapping
"Less" from another launcher wants is Less, and setup already asks for the home
role on its own.

**`LAUNCHER` on `MainActivity`.** One category. Tapping Less opens Less, which
runs first-run setup, which asks for the home role — the same path the user
would have taken through settings, reached from where they already are.

The cost is that Less then comes back in its own `getActivityList` query, so
`AndroidLauncherRepository` filters its own package out of the installed-app
list. Less is the Drawer; it is not an app in it.

An icon that is actually shown has to be worth showing, so the flat vector became
an adaptive icon with background, foreground and a monochrome layer. The
monochrome layer matters twice over: Less tints other apps by that layer in
Tinted Icon Mode, and shipping without one would have made Less the app that
ruins the effect it sells.
