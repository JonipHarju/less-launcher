# 03 — The navigation bar wears a bright scrim on light Themes

**Lane:** B (work before `04`)

## Agent Brief

**Category:** bug
**Summary:** Android enforces its own contrast scrim behind the navigation bar when the system
bars are in light appearance, so on light Themes the bottom of the screen is an opaque bright
band while dark Themes stay fully transparent.

**Current behavior:**
The launcher activity goes edge-to-edge and then sets the status and navigation bar appearance
from the active Theme: a Theme whose text colour is dark is treated as a light surface, so the
system bars are asked to draw dark icons.

Asking for light appearance also opts into the platform's *enforced navigation bar contrast* —
a translucent scrim the system paints behind the gesture bar so its handle stays visible. It is
applied only in light appearance. The result is an asymmetry the user sees immediately: on dark
Themes the Wallpaper runs to the bottom edge of the screen, and on light Themes it is cut off by
a bright band that belongs to no Theme.

This contradicts ADR-0001, which is explicit that the launcher draws over the real system
Wallpaper rather than painting a background of its own.

**Desired behavior:**
The Wallpaper reaches the bottom edge of the screen on every Theme. Neither the navigation bar
nor the status bar contributes a background of its own; the Theme's Scrim is the only thing
between the Wallpaper and the content, exactly as ADR-0001 describes.

Icon appearance must keep working as it does now — dark icons over light Themes, light icons
over dark Themes — since that is what keeps the gesture handle and status icons legible. Only
the system-supplied background goes.

Verify legibility on a light Theme over a light-toned Wallpaper before calling this done: if the
gesture handle becomes genuinely invisible, the Theme's Scrim is the correct place to solve it,
not a system-owned band.

**Key interfaces:**
- The edge-to-edge setup in the launcher activity, and the enforcement flag on the window that
  governs whether the platform paints its own navigation bar contrast.
- The existing appearance derivation from the Theme's text colour luminance — it is right and
  should be left as it is.

**Acceptance criteria:**
- [ ] On a light Theme, no opaque or translucent band is drawn behind the navigation bar by the
      system; the Theme's own Scrim gradient runs to the bottom edge.
- [ ] On a dark Theme, the bottom edge renders exactly as it does today.
- [ ] Status bar and navigation bar icons remain dark on light Themes and light on dark Themes.
- [ ] Switching Theme at runtime updates the bars without needing a restart, as it does now.

**Out of scope:**
- Re-tuning any Theme's Scrim colours or alpha values.
- The separate complaint that the Drawer and Settings read brighter than the Wallpaper on light
  Themes — that is a Scrim value question and is not part of this batch.
