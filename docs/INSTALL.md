# Installing Less on a phone

Less is not on any store. You build it and you install it yourself. The whole
thing takes about five minutes.

Requirements: an Android phone on **Android 13 or newer** (Less is `minSdk 33`,
because Tinted Icon Mode needs the monochrome icon layer introduced there).

## 1. Build the APK

    ./gradlew assembleRelease

The signed APK lands at `app/build/outputs/apk/release/app-release.apk`.

Signing is read from `keystore.properties` at the repo root. That file is
untracked and points at a keystore outside the repo. Both are covered in
[Keeping the signing key](#keeping-the-signing-key) below — read that section
before you reinstall a phone, because losing the key costs you your
configuration.

## 2. Get it onto the phone

**With a cable**, if you have USB debugging on:

    adb install app/build/outputs/apk/release/app-release.apk

That is the whole step — `adb` handles the permission prompt for you.

**Without a cable**, move the `.apk` to the phone however you like — a cloud
drive, an email to yourself, a USB file transfer. Then open it in the phone's
Files app and tap it. Android will say the app came from an unknown source and
offer a settings toggle; allow the app you opened it from (usually Files or
Chrome) to install apps, then come back and tap the file again.

## 3. Make Less your home screen

Less asks you to do this during first-run setup, right after you pick a Theme.
Accept the prompt and you are done.

If you skipped it, the Drawer keeps a reminder at the top until Less holds the
role, and you can also set it by hand:

**Settings → Apps → Default apps → Home app → Less**

The exact path varies by manufacturer. On Samsung it is Settings → Apps →
Choose default apps → Home app.

## 4. First run

Less walks you through three things, in this order:

1. **Pick a Theme.** Six ship. Four are built on public-domain paintings from
   the National Gallery of Art, one is near-black, one is off-white. Picking a
   Theme does not touch your system Wallpaper — there is a separate control for
   that in settings if you want everything to match.
2. **Make Less the default launcher**, as above.
3. **Pick your Favorites.** A small set is proposed — phone, messages, camera,
   browser, resolved from whatever apps you actually have. Accept it or change
   it.

After that: swipe up for the Drawer and start typing, long-press an app anywhere
to pin, rename, hide, or uninstall it, long-press empty space on Home for
settings.

## Going back to your old launcher

Same place you set it: **Settings → Apps → Default apps → Home app**, and pick
the launcher you had before. Less stays installed and keeps its configuration.

Uninstall it the ordinary way — but switch the home app away from Less first,
or you will be left on a phone with no home screen until you pick one.

## Updating later

    git pull
    ./gradlew assembleRelease
    adb install -r app/build/outputs/apk/release/app-release.apk

`-r` reinstalls in place and keeps your Favorites, hidden apps, and Theme. This
works only while the APK is signed with the same key.

## Keeping the signing key

Android identifies an app by its signature. An APK signed with a different key
is a different app as far as the phone is concerned: it refuses to install over
the existing one, and the only way through is to uninstall first, which deletes
your configuration.

So the key matters:

- The keystore lives at `~/.android/less-release.jks`.
- Its passwords are in `keystore.properties` at the repo root.
- Neither is in git, and neither should be. Both are in `.gitignore`.
- Back both up somewhere you will still have them after a laptop reinstall.

If you do lose the key, it is recoverable but not free: export your
configuration first (settings → "Export to a file"), uninstall, install the newly signed
build, and import the file back.

## If something goes wrong

**"App not installed"** — almost always a signature clash with an already
installed copy. Uninstall the old one and try again.

**The home button still opens the old launcher** — the home-app role did not
change. Set it by hand in step 3.

**The Drawer is missing apps** — Less lists every launchable app from every
available profile. If something is absent, check whether you hid it:
settings → "Hidden apps" lists everything you have hidden, and unhides it.
