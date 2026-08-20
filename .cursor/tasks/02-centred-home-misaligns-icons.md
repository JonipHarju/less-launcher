# 02 — Centred Home leaves app icons at ragged positions

**Lane:** A (work after `01`)

## Agent Brief

**Category:** bug
**Summary:** With Home Alignment set to Centred, each Favorite's icon and label are centred
together as one group, so the icons land at a different horizontal position on every row.

**Current behavior:**
Home Alignment has two values, Left and Centred. Centred arranges each Favorite row by centring
the icon and the label as a single group. Because labels differ in length, every row's group is
a different width, and so every row's icon sits at a different x-position. Read down the column,
the icons stagger instead of forming a line.

This is currently deliberate — the arrangement carries a comment saying centred Home centres icon
and label as one group rather than centring the label alone. In use it reads as a layout bug.

**Desired behavior:**
With Centred alignment, app icons line up in a single vertical column, and the labels read from
a consistent position beside them, regardless of label length. The rows should look like a list,
not like a ragged edge.

Left alignment is already correct and must not change.

The Favorites' block as a whole should still sit centred within Home — it is the icons within
that block that must not stagger row to row.

**Key interfaces:**
- `HomeAlignment` — the two-value setting. No change to the type or its stored representation.
- The private mapping from `HomeAlignment` to a row arrangement, and the one to a horizontal
  alignment for the containing column. The row arrangement is where the staggering originates.
- The existing comment justifying the current behaviour must be replaced, not left contradicting
  the new code.

**Acceptance criteria:**
- [ ] Under Centred alignment, two Favorites with labels of very different lengths render their
      icons at the same x-position. Assert on positions, not by eye.
- [ ] Under Centred alignment with Icon Mode off, labels remain centred and nothing reserves
      empty space where an icon would have been.
- [ ] Left alignment renders identically to before this change.
- [ ] Tombstone rows, which carry no icon, stay aligned with the labels of the rows around them.
- [ ] `./gradlew verifyRoborazziDebug` passes; if a Theme screenshot legitimately changes,
      re-record it and say so in the commit message.

**Out of scope:**
- Any change to icon size, the icon-to-label gap, or Icon Mode.
- The aesthetic complaint that the icons themselves look poor — that is a separate judgement
  and is not being made here.
