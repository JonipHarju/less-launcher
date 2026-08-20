# Prompts to start a cloud agent

Copy one of these. They are written to be pasted as-is.

---

## One task (the usual case)

> Work task `01` from `.cursor/tasks/`.
>
> Read `.cursor/tasks/GUIDE.md` first and follow it — it covers this repo's vocabulary rules,
> its testing layout, its commit style, and its branch rules. Then read
> `.cursor/tasks/01-home-drops-taps-and-drags.md`, which is the brief you are implementing, and
> `CONTEXT.md`, whose definitions are binding.
>
> Work the brief and nothing else. Stay inside the files its lane owns — the lane table is in
> `.cursor/tasks/README.md`. Cut a branch, write a failing test first, make it pass, and make
> sure `./gradlew build test verifyRoborazziDebug ktlintCheck` all pass before you commit.
>
> Commit in this repo's style: imperative subject with no prefix, and a prose body explaining why
> the change is what it is. Do not add a `Co-authored-by` trailer.
>
> If you hit a question the brief does not answer, stop and report it rather than guessing.

Swap the task number and filename for whichever one you are starting.

---

## A whole lane (sequential, one agent)

> Work lane B from `.cursor/tasks/`, in order: `03`, then `04`.
>
> Read `.cursor/tasks/GUIDE.md` and `CONTEXT.md` first; the guide is binding and the CONTEXT
> vocabulary is binding. `.cursor/tasks/README.md` has the lane table.
>
> Finish `03` completely — tests written, `./gradlew build test verifyRoborazziDebug ktlintCheck`
> green, committed on its own branch — before starting `04`. One branch and one commit per task,
> not one for the lane.
>
> Stay inside lane B's files. Other agents are working the other lanes in parallel, and reaching
> outside your lane will conflict with them.
>
> Commit style: imperative subject, no prefix, prose body saying why. No `Co-authored-by` trailer.
>
> Report at the end: what you changed per task, what each acceptance criterion is satisfied by,
> and anything you had to leave undone.

---

## Running the whole batch in parallel

Start one agent per lane, each with the lane prompt above. The lanes and their tasks:

| Lane | Tasks | Prompt with |
| --- | --- | --- |
| A | `01`, `02` | "Work lane A, in order: `01`, then `02`." |
| B | `03`, `04` | "Work lane B, in order: `03`, then `04`." |
| C | `05` | "Work task `05`." |
| D | `06` | "Work task `06`." |
| E | `07`, `08` | "Work lane E, in order: `07`, then `08`." |

No two lanes touch the same file, so five agents can run at once without conflicting.

Two notes before you launch them:

- **`01` is the one that matters most.** It is the reason taps on Home do nothing and the reason
  dragging a Favorite appears unimplemented. If you only run one lane, run A.
- **`08` contains a design judgement** the agent's brief-writer made rather than one you gave.
  It is flagged at the top of that file. Read it before launching lane E if you want the call to
  be yours.
