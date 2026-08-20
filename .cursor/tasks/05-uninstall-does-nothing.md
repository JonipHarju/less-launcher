# 05 — Uninstall from Curation does nothing

**Lane:** C (the only task in its lane)

## Agent Brief

**Category:** bug
**Summary:** The uninstall action offered by Curation fires a delete intent that package
visibility prevents from resolving, and the failure is swallowed, so choosing Uninstall has no
visible effect at all.

**Current behavior:**
Curation offers Uninstall for one app, on both Home and the Drawer. Choosing it asks the system
to delete that package through the ordinary delete action with a package URI, started through a
helper that catches the not-found exception and returns false, which no caller inspects.

The app targets a modern SDK, so package visibility filtering is fully in force, and the manifest
deliberately avoids the query-all-packages permission (ADR-0006). The `queries` block declares
the launcher intent, the dial action and the still-image-camera action — and nothing that would
let the launcher see the package installer. The delete intent therefore resolves to nothing, the
exception is caught, and the menu closes as though the request had been accepted.

**Desired behavior:**
Choosing Uninstall brings up the system's uninstall confirmation for the chosen app. Confirming
it uninstalls the app; declining leaves it installed. Either way the launcher's own list catches
up, because the installed-app list is driven live from package broadcasts (ADR-0003) rather than
cached.

The uninstall must remain the platform's, not the launcher's — the launcher asks and the system
decides, which is what makes a work-profile app decline rather than uninstalling its personal
twin.

Where the request genuinely cannot be made — a device with no package installer, or a package the
system refuses to let the launcher act on — the user must be told rather than left with a menu
that closed and did nothing.

Whatever visibility declaration this needs belongs in the manifest's `queries` block with a
comment explaining why, in the style of the declarations already there. The merged-manifest test
asserting the permission set must stay green: this is a visibility problem, and adding a
permission is the wrong fix.

Task `04` in this batch may also add an entry to the same block. Merge cleanly rather than
overwriting.

**Key interfaces:**
- `LauncherRepository.requestUninstall(appId)` and its Android implementation. The signature can
  change if reporting failure requires it, but the repository seam stays: Curation asks the
  repository, never the platform directly.
- The private helper in the Android repository that starts an intent and reports whether it
  resolved — its boolean result is currently discarded.
- The manifest's `queries` element.
- The fake repository used by tests, which must gain whatever the interface gains.

**Acceptance criteria:**
- [ ] Choosing Uninstall from Curation on Home produces the system uninstall confirmation.
- [ ] Choosing Uninstall from Curation in the Drawer does the same.
- [ ] After an app is uninstalled, it leaves the Drawer without the launcher being restarted.
- [ ] A Favorite the user uninstalls leaves **no** Tombstone — CONTEXT is explicit that a
      Tombstone marks a loss the user did not ask for. Add a test if none covers this.
- [ ] When the request cannot be made at all, the user sees a dismissible message.
- [ ] The manifest declares no new permissions and the existing permissions test passes.
- [ ] `./gradlew test` and `./gradlew ktlintCheck` pass.

**Out of scope:**
- The Curation menu's contents or wording.
- App info, which uses a different platform call and works.
- Bulk or multi-select uninstall.
