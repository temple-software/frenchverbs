# Authoritative design assets

`launcher-template.xml` is the neutral vector master: an indigo construction tile with a T
and aqua placeholder blocks. It is deliberately generic, not production product artwork.
Regenerate identical Android launcher/splash resources with:

```sh
python3 scripts/generate_launcher_icons.py
```

No raster libraries or generated AI assets are needed. A future product replaces this master
with its own approved artwork and adapts the generator as needed. Preserve authoritative
master launcher artwork here. Android adaptive/legacy launcher assets, the separate Play
Store icon, feature graphics and screenshots are distinct outputs with different requirements.
Keep localized store assets separate and verify actual dimensions. This template includes
no store feature graphics/screenshots and reuses no product-specific reference artwork.

Official reusable brand originals and provenance live in nice-education/; Android nice_*.xml
vectors are exact accepted conversions. Preserve logo aspect ratio and genuine sun paths.
Do not copy a product clock, splash or store illustration into the generic brand collection.

For each product use design/launcher/, design/splash/, design/store/ and store/en-GB/, store/fr/
for editable originals, exports, listing copy and screenshots. These product assets are not
supplied by this neutral template. Store icon 512×512 and feature graphic 1024×500 are separate
outputs; recheck current Play rules when publishing. Adaptive icon 108dp layers have a central
66dp safe zone for essential details. Decorative background may reach the mask; inspect actual
circular/squircle/rounded-square launcher masks and newly pinned shortcuts, not prediction UI.
Do not stack background colours into unintended rings or enlarge essential numerals until clipped.
