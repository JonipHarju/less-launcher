# 01 — Home silently drops taps, long presses and drags

**Lane:** A (work before `02`)

## Agent Brief

**Category:** bug
**Summary:** The unified tap/long-press/drag detector abandons the gesture whenever another
pointer consumer takes the movement, so tapping a Favorite on a real device frequently does
nothing at all.

**Current behavior:**
Every Favorite row on Home reads its touches through a single `Modifier.onTapLongPressOrDrag`
detector, which exists so launching, curating and dragging never race for the same touch. It
waits for an up-event within the long-press timeout; a timeout means the press was long and
becomes a curate-or-drag, and a release within the timeout is a tap.

The wait returns null in two different situations that the detector treats as one: the pointer
was released, and the pointer was **consumed by another handler**. Home's Favorites sit inside
a vertically scrolling container, so the smallest finger drift during a tap lets the scroll
handler consume the pointer. The detector then falls through its tap branch with nothing to
release, returns, and fires no callback whatsoever — not the tap, not the long press, not the
drag.

Reported symptoms, all one cause: tapping an app on Home often does nothing, and dragging a
Favorite to reorder it appears not to be implemented (it is — the reorder path is fully wired).

The existing instrumented tests pass because a synthetic click travels zero pixels; a thumb
does not.

**Desired behavior:**
A press on a Favorite always resolves into exactly one outcome, and small incidental movement
never dissolves it:

- A press released without travelling beyond touch slop launches the app, even if the pointer
  drifted a few pixels first.
- A press held past the long-press timeout and then released without travelling opens Curation.
- A press held past the timeout and then dragged vertically reorders the Favorite and commits
  on release.
- Deliberate scrolling of Home still scrolls, and must not launch whatever was under the finger.

The row must win the pointer for the gestures it owns rather than yielding it to the scrolling
container. Distinguishing "released" from "consumed elsewhere" is the crux — today they are the
same null.

**Key interfaces:**
- `Modifier.onTapLongPressOrDrag(key, onTap, onLongPress, onDrag, onDragEnd)` — the detector.
  Its five-callback contract is correct and should be preserved; the pointer handling inside it
  is what is wrong.
- `ViewConfiguration.touchSlop` — the threshold that separates a drifting tap from a scroll, and
  the one the tap path currently does not consult at all.
- Home's scrolling container and the swipe handlers that open the Drawer — the other claimants
  on the same pointer stream. The fix must not break the swipe that opens the Drawer, nor Home's
  ability to scroll once Favorites outgrow the screen.

**Acceptance criteria:**
- [ ] A tap that moves less than touch slop before release launches the Favorite. Cover this
      with a test that injects a press, a small move, and a release — not a zero-travel click.
- [ ] A press past the long-press timeout, released in place, opens Curation for that Favorite.
- [ ] A press past the long-press timeout followed by vertical travel reorders the Favorite and
      persists the new order on release.
- [ ] A vertical drag that begins before the long-press timeout scrolls Home and launches nothing.
- [ ] The swipe that opens the Drawer still opens it, in both configured directions.
- [ ] No gesture path exists that fires none of the four callbacks. Assert this directly.
- [ ] Tombstone rows keep their behaviour: long press dismisses, tap does nothing.

**Out of scope:**
- Making dragging discoverable (an affordance, a hint, a handle). That is a separate design
  question; this task is only about the gesture working when performed.
- The Drawer's rows, which use an ordinary combined-clickable and are not affected.
- Any change to how reordering is persisted — that path works.
