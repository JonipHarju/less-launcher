# 08 — Settings gives almost no feedback that a choice registered

**Lane:** E (work after `07`)

## Agent Brief

**Category:** enhancement
**Summary:** A chosen option in Settings is distinguished from an unchosen one only by text
colour, which is too weak a signal to read as confirmation, so the user cannot tell whether a tap
selected anything.

> **Judgement call flagged for the maintainer.** The problem is confirmed; the remedy below is
> the agent's brief-writer choosing a direction, not the maintainer's stated preference. It is
> constrained to the vocabulary the codebase already uses so it is cheap to change or revert.

**Current behavior:**
Every option group in Settings — the Drawer's opening direction, Home Alignment, Icon Mode — is a
list of plain text lines. The chosen one is drawn in the Theme's primary text colour and the rest
in its secondary text colour. That is the entire difference. On several Themes the two colours
are close, and with nothing else to compare against, a line does not read as "selected" so much
as slightly darker.

The Theme picker has the same problem in a stronger form: each option is a four-line Credit, and
the only marker of which Theme is active is that two of its lines are in the primary colour.

The on/off settings do better — they spell out "On" or "Off" beside the label — and are the
existing precedent for what good feedback looks like here.

**Desired behavior:**
Selection is legible at a glance on every Theme, without the user comparing one line against
another.

Each option group marks its chosen option with an explicit visible marker in addition to the
colour it already uses, so that a single option read in isolation still says whether it is the
chosen one. Use the glyph vocabulary the interface already speaks — the close cross, the gear,
the move arrows, the unhide return are all single characters rendered as text, in the Theme's
own colours — rather than introducing an icon set, a control library, or a component style that
appears nowhere else.

Apply this consistently to the fixed-list option groups and to the Theme picker. Unchosen options
must reserve the marker's space so that choosing one does not shift the layout.

The marker is a redundancy on top of the colour difference, not a replacement for it, and it must
carry a selection state to the accessibility layer as well: these controls already declare
themselves selectable, and the Theme picker already declares itself a radio button with a merged
description. Keep those semantics correct.

**Key interfaces:**
- The private generic option-group composable in Settings, which every fixed-list setting routes
  through — one change there covers Drawer opening direction, Home Alignment and Icon Mode.
- The Theme picker composable, which builds its own selectable rows rather than using that group.
- The shared control composables, which are where a reusable single-character marker belongs if
  one is worth extracting.
- The on/off row, as the precedent for how this interface states a value. Do not change it.

**Acceptance criteria:**
- [ ] In every fixed-list option group, the chosen option carries a visible marker the unchosen
      options do not.
- [ ] The Theme picker marks the active Theme the same way.
- [ ] Choosing a different option moves the marker and does not shift any other layout.
- [ ] The marker uses the Theme's own colours and font and is legible on all six Themes.
- [ ] Selection state is exposed to accessibility for every option group and for the Theme picker,
      and the Theme picker keeps its merged single-target semantics.
- [ ] `./gradlew verifyRoborazziDebug` passes; re-record and say so if a Theme screenshot changes
      legitimately.

**Out of scope:**
- Restructuring Settings, splitting it across surfaces, or moving Themes elsewhere. CONTEXT is
  explicit that there is exactly one Settings screen and options are never scattered.
- Toasts, snackbars, or any transient confirmation that appears and disappears.
- Adding a component library or icon set to the project.
