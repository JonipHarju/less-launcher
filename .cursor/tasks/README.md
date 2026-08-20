# One-off task queue

A batch of agent briefs written from a round of hand-written feedback on 2026-08-20,
for cloud agents that cannot reach this repo's GitHub issues.

**This folder is disposable and is not a second issue tracker.** The tracker of record is
still GitHub issues in `JonipHarju/less-launcher` — see `docs/agents/issue-tracker.md`.
Nothing here changes that. Delete this folder once the batch has landed.

## The three files that are not tasks

- **`GUIDE.md`** — how to work in this repository: the binding vocabulary, the ADRs, the test
  layout, the commands, the commit and branch style. **An agent reads this first.**
- **`PROMPT.md`** — prompts to paste into a cloud agent to start one task or one lane.
- **`README.md`** — this file: the index, the lanes, and what is deliberately out of scope.

## How to work these

Each numbered file is a self-contained agent brief. Read `GUIDE.md`, then `CONTEXT.md`, then
your brief. Pick one task, honour its lane order, and finish it before starting another.

If `/implement` is available it drives the loop for you; `GUIDE.md` spells the loop out for
when it isn't.

Every change must pass what CI runs:

    ./gradlew build
    ./gradlew test
    ./gradlew verifyRoborazziDebug
    ./gradlew ktlintCheck

## Lanes

Tasks are grouped into lanes by the files they touch, so agents in different lanes can run
in parallel without conflicting. **Within a lane, work in order.** Do not start a task in a
lane another agent is already working.

| Lane | Tasks | Touches |
| --- | --- | --- |
| A — Home gestures | `01`, `02` | the tap/long-press/drag detector, Home |
| B — Clock and window | `03`, `04` | the launcher activity's window setup and outbound intents |
| C — Uninstall | `05` | the manifest's `queries`, the Android repository |
| D — Drawer search | `06` | Drawer |
| E — Settings | `07`, `08` | Settings, the Configuration file feature, shared controls |

## Out of scope for this batch

These came from the same feedback round and were deliberately **not** turned into tasks.
Do not pick them up, and do not fold them into a task that sits near them:

- **A user-facing Theme creator.** `CONTEXT.md` states Themes are authored, not
  user-editable, and `Theme` is a fixed preset. Changing that is a spec decision, not a
  ticket.
- **Moving Themes out of Settings.** `CONTEXT.md` states there is exactly one Settings
  screen and options are never scattered across surfaces.
- **An alphabet index down the side of the Drawer.** Well-liked idea, but the interaction
  is unspecified — whether it jumps or filters, what it does while a search query is
  active, whether it appears for a short list.
- **"The icons look bad."** Task `02` fixes the one part of this with a defined answer
  (their alignment). The aesthetic judgement is not an agent's to make.
- **The Drawer and Settings Scrim reading brighter than the Wallpaper on light Themes.**
  Real, and deliberate per ADR-0001 — the light Themes carry a near-opaque light Scrim at
  0.94/0.90 alpha. Re-tuning those values is a taste call.
