Superseded by [ADR-0010](0010-the-configuration-travels-with-automatic-backup.md): the
exported file is gone; automatic backup of the stored proto still stands.

# The exported Configuration is the stored proto, behind a format version

The file the user exports holds the same proto records the launcher stores,
wrapped in a message carrying a format version. Import parses it, refuses any
version but its own, and writes the whole Configuration in one DataStore write.

A human-readable export — JSON, or a list of package names — reads better and
is worth less. What the user is restoring is component names, profile serial
numbers and positions; none of it is text they would edit, and every format
that isn't the storage format is a second schema to keep in step with the first.
The version field is what a plain copy of the stored message would lack: it
lets a later Less refuse a file it cannot read rather than half-understand it,
and it makes a file the user picked by mistake fail to parse instead of
importing garbage.

Two things stay out of the file, because they are the device's answers and not
the user's choices: how far Setup got, and whether Less has held the Home Role.
Carrying them would let a Configuration exported before Setup finished send a
restored phone back through Setup on a launcher it already trusts.

Android's automatic backup cannot make that distinction — it carries the stored
file whole — so a restore agent clears the Home Role answer once the platform
has put the file down. Without it a restored phone would come up believing Less
had once been the default launcher, and the Drawer's standing prompt, which is
the only way in, would never appear.

What the file cannot carry across phones is the profile serial number a Favorite
records. It is the device's own numbering, so a work-profile Favorite exported
from one phone names a profile another phone does not have and imports as a
Tombstone the user can dismiss. Personal-profile apps, which is nearly all of
them, serialise to zero everywhere and come back intact. Remapping the rest
would mean guessing which profile on the new phone answers to which on the old,
and a wrong guess puts an app on Home that the user never pinned.
