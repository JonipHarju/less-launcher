# The Configuration travels with automatic backup, not a file the user writes

Less used to let the user write their Configuration to a file and read it back.
The file was the stored proto wrapped in a format version (ADR-0007), so that a
later Less would refuse a copy it could not read. Automatic backup already
carries those same records to a restored phone. The extra file did not earn the
Settings controls, the picker round-trip, or a second schema to keep in step
with storage, and it is withdrawn.

The stored records and their field numbers stay. So does the automatic-backup
half of ADR-0007: the platform copies the stored file whole, so the restore
agent still clears the Home Role answer after a restore. Without that, a
restored phone would come up believing Less had once been the default launcher,
and the Drawer's standing prompt — the only way in — would never appear.

This supersedes ADR-0007.
