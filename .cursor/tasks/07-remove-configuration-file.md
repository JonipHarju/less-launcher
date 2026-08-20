# 07 — Remove the Configuration import and export

**Lane:** E (work before `08`)

## Agent Brief

**Category:** enhancement
**Summary:** Drop the user-facing ability to write a Configuration to a file and read it back,
along with the export format that exists only to serve it, and record the reversal in an ADR
that supersedes ADR-0007. **Android's automatic backup stays.**

**Why this needs care:** ADR-0007 is an argument *for* this feature and the shape of its file
format. This task overturns half of that decision, on the maintainer's explicit call: the manual
file was judged not to earn its place. Overturning a recorded decision means writing the reversal
down, not quietly deleting the code and leaving an ADR that describes something the repository no
longer contains.

**Current behavior:**
Settings ends with a Configuration group offering Export and Import. Export asks the system's
file picker for a destination and writes the encoded Configuration to it; Import asks for a file,
refuses anything above a size limit or carrying the wrong format version, and writes the whole
Configuration in one store write. Both report success or failure through a dismissible message.

Underneath, the export format is the stored proto records wrapped in a message carrying a format
version, so that a later version of the launcher refuses a file it cannot read rather than
half-understanding it.

Separately and independently, Android's automatic backup carries the stored file to a restored
phone, and a restore agent then clears the Home Role answer so that a restored phone does not
come up believing the launcher had once been the default. That mechanism is **not** part of this
removal.

**Desired behavior:**
Settings no longer offers Export or Import, and the Configuration group disappears with them.
Nothing else in Settings moves or changes order.

The code that existed only to serve those two controls goes with them: the file reading and
writing, the size limit, the format-version check, the encode and decode of the versioned wrapper
message, the versioned message in the proto schema, the repository call that restores a whole
Configuration from outside, and the strings and tests belonging to all of it. Leave no dead code
behind for a later reader to puzzle over.

What stays, untouched:

- The stored proto that the launcher actually persists to, and every record in it. Only the
  versioned *export wrapper* message goes.
- Automatic backup: the backup flags in the manifest, the extraction rules, the restore agent and
  its clearing of the Home Role. A restored phone must still behave exactly as it does today, and
  the reasoning for that must survive in the ADR record.

**Documentation this task must also do:**

- Add `docs/adr/0010-*.md` superseding ADR-0007, in the voice and shape of the existing ADRs:
  what was decided before, what is decided now, and why the manual file did not earn its keep
  when automatic backup already carries the same records. State plainly that the automatic-backup
  half of ADR-0007 — including why the restore agent clears the Home Role — still stands.
- Mark ADR-0007 as superseded by the new one, following whatever convention the existing ADRs
  use; if they have no such convention, add a short line at its top and keep the file intact.
- Update the **Configuration** entry in `CONTEXT.md`, which currently says the user "can
  additionally write it to a file of their own and read it back". That sentence becomes untrue.
  Keep the rest of the definition, including what is deliberately left out of a restore.

**Key interfaces:**
- The Settings composable, and the Configuration file composable it calls last.
- `LauncherRepository.configuration()` and `LauncherRepository.restoreConfiguration(...)`, plus
  the fake repository used in tests — remove what only import and export used, keep anything the
  restore agent or the store still needs.
- The encode and decode helpers for the versioned export, the format-version constant, and the
  `ExportedConfiguration` message in the proto schema.

**Acceptance criteria:**
- [ ] Settings shows no Configuration group, no Export and no Import.
- [ ] No unreferenced code, strings or test fixtures remain from the removed feature.
- [ ] The stored proto still round-trips every setting, Favorite and Hidden App as before.
- [ ] Automatic backup and the restore agent are unchanged, and their tests still pass.
- [ ] `docs/adr/0010-*.md` exists, supersedes ADR-0007, and explains the reversal.
- [ ] ADR-0007 is marked superseded.
- [ ] `CONTEXT.md`'s Configuration entry no longer claims the user can export a file.
- [ ] `./gradlew build test verifyRoborazziDebug ktlintCheck` all pass.

**Out of scope:**
- Removing or weakening automatic backup in any way.
- Changing the stored proto's own records or their field numbers.
- Any other Settings option.
