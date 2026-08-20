# Working guidelines for this repository

Read this before touching a task in `.cursor/tasks/`. It is the house style, stated once, so
each brief can stay about its own bug.

## Read these first, in this order

1. **`CONTEXT.md`** — the project's vocabulary. Not background reading; see below.
2. **`AGENTS.md`** — points at the repo's agent configuration.
3. **`docs/adr/`** — ten short decision records. Read the ones touching your task's area.

## The vocabulary is binding

`CONTEXT.md` defines the project's terms — Home, Drawer, Setup, Settings, Favorite, Tombstone,
Curation, Theme, Scrim, Icon Mode, Home Role, Soft Cap and the rest. Each entry ends with an
_Avoid_ list of synonyms that are **not** to be used.

Use the defined words in code, in comments, in tests, in commit messages. Do not write "app
drawer", "home screen page", "context menu" or "pinned app". A type or function named after a
word the glossary does not have is a defect here, and has been fixed as one before.

If your task seems to need a concept the glossary has no word for, that is a signal to stop and
say so — not to invent one.

## The ADRs are binding too

An ADR records a decision and the reasoning behind it. Several exist specifically to stop a
future reader from "fixing" something deliberate. Before changing anything that an ADR covers,
read it.

If a task requires contradicting an ADR, the task will say so explicitly and will tell you to
write a superseding ADR. **Only task `07` does this.** If you find yourself contradicting an ADR
that your brief never mentioned, stop — you have misread the task.

## Working a task

Pick one file from `.cursor/tasks/`, honour the lane order in `README.md`, and work it to
completion before picking another.

If `/implement` is available to you as a skill, use it — it drives `/tdd` and closes with
`/code-review`. If it is not, run that loop by hand:

1. Read the brief in full, plus `CONTEXT.md` and any ADR it names.
2. Explore the area before changing it. The brief names types and functions, never file paths —
   find them.
3. Write a failing test that captures the defect or the missing behaviour. For a bug, it must
   fail for the stated reason before you touch the fix.
4. Make it pass. Change no more than the brief asks.
5. Review your own diff on two axes: does it match the brief, and does it match this guide?
6. Commit.

## Tests

Three source sets, and choosing the right one matters here:

- **`app/src/test/`** — plain Kotlin unit tests, Robolectric tests, and the Roborazzi Theme
  screenshots. Fast, no device. **This is where logic belongs.**
- **`app/src/androidTest/`** — instrumented tests on a device or emulator. Reserved for what
  genuinely needs one: gesture detection and wiring.
- **`app/src/testFixtures/`** — `FakeLauncherRepository`, shared by the tests. If you change the
  `LauncherRepository` interface, the fake changes with it.

The house preference, visible throughout the history: **pull logic out into pure modules that can
be driven as plain Kotlin, and leave the instrumented tests covering only the parts that truly
need a device.** A test that needs a device to check a rule is a sign the rule is in the wrong
place. Prefer adding to a pure module over adding a Compose test.

Every Theme is covered by a screenshot test, and those are the only thing that will catch a Theme
becoming unreadable. If your change legitimately alters one, re-record it and say so in the
commit message. Never re-record to make a failure go away without understanding it.

## Commands

```
./gradlew build                 # what CI builds
./gradlew test                  # unit, Robolectric and screenshot tests
./gradlew verifyRoborazziDebug  # Theme screenshots against the recorded set
./gradlew ktlintCheck           # lint
```

All four must pass. Two more you will want:

```
./gradlew ktlintFormat          # fix formatting
./gradlew recordRoborazziDebug  # re-record screenshots, only when a change is intended
```

## Code style

- ktlint governs formatting. `.editorconfig` exempts `@Composable` functions from function
  naming, which is why they are capitalised.
- **KDoc says why, not what.** Read the existing headers: they explain a decision, a constraint,
  or a trap, and they read as prose. `/** Returns the favorites. */` is noise; the comment
  explaining that Home keys a row by app rather than by position so a dragged row keeps its
  live touch is the standard to match.
- Inline comments are for the non-obvious, and this codebase uses them well. Where you make a
  choice a reader would question, say why in one line.
- Match the density and voice of the file you are editing.

## Commits

This repository has a distinctive commit style. Match it.

- **Subject:** imperative mood, sentence case, no prefix and no
  `feat:`/`fix:`/`chore:` tag. "Bring the Home Role rule behind the repository seam", not
  "fix: home role bug".
- **Body:** real prose, wrapped, explaining *why* the change is what it is — the reasoning, the
  thing that was actually wrong, and anything you deliberately left alone and why. Bodies here
  run several paragraphs. A one-line commit for a real change is out of place.
- **No co-author trailers.** Do not add `Co-authored-by: Cursor`, or any other agent attribution.
  The sole author is the repository owner and the account making the commit. This is a standing
  rule from the maintainer; earlier commits that carry such a trailer are not the precedent to
  follow.

## Branches

One branch per task, cut from the branch you were started on. Name it for what it does, in
kebab-case, with no prefix — `home-drops-taps`, `uninstall-visibility`. Look at the existing
branch names and match them.

**Never commit to `main`.**

## Lane discipline

`README.md` groups tasks into lanes by the files they touch. Agents in different lanes run in
parallel safely. Working two tasks from different lanes on one branch, or reaching outside your
lane's files "while you are in there", produces conflicts the maintainer resolves by hand.

Stay in your lane. If a fix genuinely requires touching another lane's files, stop and say so
rather than doing it.

## When you are blocked

Stop and write it down. Do not guess at a product decision.

The tasks in this batch were filtered specifically so that none of them requires one — the
feedback that needed the maintainer's judgement was left out of the queue on purpose, and is
listed at the bottom of `README.md`. So if you hit a question that feels like taste, scope, or
"what should this look like", something has gone wrong: either you have drifted outside the
brief, or the brief has a genuine gap.

Either way, the useful output is a clear note in your final report saying what you needed and
what you would have had to assume. A half-finished task with a good question beats a finished
one built on a guess.

## Definition of done

- The brief's acceptance criteria are each satisfied, and you can say how.
- `./gradlew build test verifyRoborazziDebug ktlintCheck` all pass.
- The diff contains nothing the brief did not ask for.
- New vocabulary, if any, matches `CONTEXT.md`.
- No ADR is silently contradicted.
- The commit message would tell the maintainer, in six months, why this change is what it is.
