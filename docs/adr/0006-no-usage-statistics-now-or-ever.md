# No usage statistics, now or ever

Less asks for the wallpaper permission and nothing else. It does not request
usage-statistics access, and it does not request device-administration access.
A test reads the merged manifest and asserts it, so the decision cannot be
undone by an unnoticed dependency or a convenient afternoon.

Usage access is what a launcher would want for the obvious features: the apps
you open most, a "recent" row, a suggestion that learns. Every one of them is a
feature Less does not have. Home is a short list the user chose deliberately,
and nothing promotes an app onto it — so reading which apps get opened would buy
nothing a Favorite does not already say, in exchange for a permission screen
that asks the user to hand over a record of everything they do on the phone.

Setup still has to propose a working Home on a device it knows nothing about. It
does that by asking the platform which app answers each Everyday Intent, which
needs no permission at all: dialling, messaging, the camera and the browser are
resolved by intent, and the device's own answers become the first Favorites.
Package visibility for those queries is declared in the manifest, so the answers
arrive without `QUERY_ALL_PACKAGES` either.
