# 04 — Tapping the clock does nothing on some devices

**Lane:** B (work after `03`)

## Agent Brief

**Category:** bug
**Summary:** Home opens the clock with a single alarm intent and silently swallows the failure
when no activity handles it, so on devices whose clock app does not answer that intent the tap
produces no response of any kind. Confirmed on a OnePlus 13R.

**Current behavior:**
Tapping the time on Home fires one intent — the platform's show-alarms action — through a helper
that catches the not-found exception and does nothing with it. The comment there notes that some
devices provide no clock activity.

Two problems. First, a device can have a perfectly good clock app that simply does not declare a
filter for that particular action, and the launcher gives up without trying anything else.
Second, when it does give up, the user gets no feedback: the tap is indistinguishable from a
dead region of the screen.

The date, which opens the calendar through a content-URI view intent, has the same silent-failure
shape and should be treated the same way.

**Desired behavior:**
Tapping the clock opens the device's clock app on any device that has one, and tapping the date
opens its calendar. Each should try a sequence of increasingly general ways to get there and use
the first that resolves, rather than betting everything on a single action.

For the clock, that means falling back from the alarm-specific action toward simply launching
whichever app the device treats as its clock. For the date, from the calendar's content URI
toward the calendar app itself.

When every attempt fails — a device genuinely has no such app — the user must be told, briefly
and in the launcher's own voice, rather than being left to wonder whether the tap registered.
There is an existing dismissible-message component for exactly this kind of advisory.

Note the package-visibility constraint: the manifest deliberately avoids the query-all-packages
permission (ADR-0006), so any new intent this resolves needs a matching declaration in the
manifest's `queries` block, with a comment saying why it is there. Task `05` in this batch edits
the same block for a different reason — if it has already landed, merge cleanly rather than
reverting its entry.

**Key interfaces:**
- The clock and calendar handlers passed into the launcher's root composable, and the private
  helper that starts an intent and swallows the not-found exception. That helper should report
  whether it succeeded rather than discarding the outcome.
- The manifest's `queries` element — package visibility, not permissions, is what decides
  whether an intent resolves at all here.
- The existing notice component used elsewhere for advisory messages the user dismisses.

**Acceptance criteria:**
- [ ] Tapping the clock opens a clock app on a device where the alarm action resolves.
- [ ] Tapping the clock opens a clock app on a device where the alarm action does **not** resolve
      but a clock app is installed.
- [ ] Tapping the date opens a calendar under the same two conditions.
- [ ] When nothing resolves, a dismissible message tells the user, and no exception escapes.
- [ ] Any newly required visibility declaration is present in the manifest with a comment
      explaining it, and the merged manifest still declares no new permissions — the existing
      test asserting the permission set must stay green.
- [ ] Unit tests cover the fallback ordering without needing a device.

**Out of scope:**
- Letting the user choose which app the clock or the date opens. Which app answers is the
  platform's decision, consistent with how Everyday Intent works.
- Any change to how the time or date is formatted or how often it ticks.
