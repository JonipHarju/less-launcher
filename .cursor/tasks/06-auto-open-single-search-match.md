# 06 — Open an app when the Drawer search narrows to exactly one

**Lane:** D (the only task in its lane)

## Agent Brief

**Category:** enhancement
**Summary:** When a Drawer search query of three or more characters matches exactly one app, that
app should launch immediately, without the user reaching for the keyboard's search key.

**Current behavior:**
The Drawer ranks the installed apps against the typed query — excluding Hidden Apps entirely, and
matching a renamed Favorite against the user's own name for it as well as its real one. The list
narrows as the user types, and pressing the keyboard's search action launches whatever ranks
first. Nothing happens automatically: a query that has narrowed to one app still waits for a
deliberate second action.

**Desired behavior:**
As the user types, the moment the query is **at least three characters** and the ranked result
set contains **exactly one** app, that app launches.

The three-character floor is the whole safety property: it stops a one- or two-letter query from
passing through a single match on the way to a longer one and launching something under the
user's finger. Below three characters nothing launches automatically, however few apps match.

Typing "out" on a device where Outlook is the only app matching "out" opens Outlook.

Rules that must hold:

- Only a **forward-typed** query triggers this. Deleting characters back down into a single match
  must not launch anything — the user is retreating, not choosing.
- It fires at most once per query. Returning to the Drawer and finding the same single-match
  query still present must not relaunch.
- Hidden Apps are already absent from the ranking and must stay irrelevant to the count.
- Pressing the keyboard's search action keeps working exactly as it does now, at any query length.

**Key interfaces:**
- The Drawer's query state and the ranking call that turns a query into an ordered list of apps.
  The count of that list, plus the query length, is the whole trigger condition.
- `LauncherRepository.launch(app)` — the same call the row tap and the search action already use.
- The decision belongs in a pure, unit-testable function of (query, ranked results, previous
  query) rather than buried in the composable — everything about it is testable without a device
  and should be.

**Acceptance criteria:**
- [ ] A query of three or more characters matching exactly one app launches that app.
- [ ] A query of one or two characters matching exactly one app launches nothing.
- [ ] A query matching two or more apps launches nothing.
- [ ] Deleting characters down to a state where exactly one app matches launches nothing.
- [ ] A query that matched one app and launched it does not launch again on recomposition or on
      reopening the Drawer.
- [ ] A Favorite's custom label still counts as a match, and Hidden Apps still count as nothing.
- [ ] The keyboard's search action still launches the top-ranked app for any query.
- [ ] The trigger rule is covered by unit tests, not only instrumented ones.

**Out of scope:**
- Changing how apps are ranked or matched.
- Any delay, debounce or animation before launching — the three-character floor is the chosen
  safety mechanism, and a timer as well would make the behaviour hard to predict.
- An alphabet index or any other new way to navigate the Drawer.
