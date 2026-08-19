# Tinted mode desaturates artwork it cannot theme

Tinted Icon Mode draws an app's themeable monochrome layer tinted to the Theme's
accent colour. Apps that supply no such layer keep their own artwork, stripped of
colour and scaled by the accent colour, so the icon's light and dark detail
survives the tint. They are not replaced with a derived silhouette.

Coverage is not high enough to do better. Measured on an emulator carrying
21 launchable apps — almost all Google's own, which reliably ship a monochrome
layer — only 15 supplied one. That 71% is an optimistic ceiling; a phone
carrying third-party apps sits below it. Deriving a silhouette from the
remaining artwork means thresholding arbitrary icons, which turns a photograph-
backed icon into a blob and a thin-line icon into nothing. Desaturating keeps
every icon recognisable and reads as one palette, at the cost of the fallback
icons carrying more internal contrast than the themed ones.
