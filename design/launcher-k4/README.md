# Approved K4/B Family Bevel launcher

The master is copied byte-for-byte from the owner-supplied ZIP; see source.json.
Run `python scripts/generate_k4_icons.py` with Pillow to regenerate exports.
This performs deterministic resizing/padding, not a redraw of the approved artwork.

Android standard and round PNGs retain alpha and use 37.33dp artwork on a 48dp canvas.
Adaptive foreground uses 56dp artwork on a 108dp canvas, with a transparent background.
Android applies its launcher mask; both words fit the central 66dp safe circle.
Android 12+ splash uses 156dp artwork on a 288dp canvas to fit the 192dp mask.
Both day/night splash themes already reference ic_splash and retain their backgrounds.
Android 24/25 use the standard/round density-specific PNGs; 26+ use adaptive XML.

The separate google-play-icon-512.png is an opaque 512px full square, sRGB PNG.
It composites the approved artwork on blue #0739A5; Android exports do not use it.
The bevel and internal shadows are part of the approved art; no outer shadow added.
Preview images show the nominal circular launcher mask and conservative safe circle.
