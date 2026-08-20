# Settings are marshalled by hand, one field at a time

`LauncherSettings` is written out and read back by hand: a field on the domain
type, a line in `toLauncherSettings`, a line in `mergedInto`, and — where the
field is an enum — a `toProto`/`toDomain` pair whose `when` names every stored
value. Adding one boolean setting touches the proto, the domain type, both
directions of the mapping, Settings, and `strings.xml`. Adding an enum setting
touches two more.

That cost is counted and accepted. It is not an oversight to be fixed the next
time someone reads `StoredConfiguration.kt`.

## Considered Options

**A generic mapping** — reflection over the domain type, or a registry pairing
each field with its stored counterpart — would collapse the two mapping lines
into the declaration and take the per-setting cost down to roughly two edits.

It would also take the exhaustive `when` with it. Those `when` expressions are
the reason an unrecognised stored value is a compile error rather than a silent
fallback: adding a value to a proto enum without teaching Less what it means
fails the build today. A registry answers `null` at runtime instead, on a device,
after a restore, where nobody is looking. The mapping is also the one place the
launcher decides that an unset field means the domain type's own default rather
than proto's zero value — `toLauncherSettings` says so field by field, and a
generic mapping would have to grow a second mechanism to express it.

**Codegen from the proto** — more machinery than seven fields justify, and it
would still need the per-field default rule expressed somewhere.

Seven fields do not pay for either. The repetition is visible, mechanical, and
caught by the compiler when it goes wrong, which is the trade worth having at
this size. Somewhere past roughly fifteen settings the arithmetic changes and
this is worth reopening — the thing to weigh then is what replaces the
compile-time exhaustiveness, not whether the boilerplate is tedious.
