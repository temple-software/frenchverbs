"""Export approved K4 artwork without redrawing it. Requires Pillow."""
from pathlib import Path
import hashlib
import json
from PIL import Image, ImageCms, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
DESIGN = ROOT / "design/launcher-k4"
SOURCE = DESIGN / "master-transparent.png"
PROFILE = ImageCms.ImageCmsProfile(ImageCms.createProfile("sRGB")).tobytes()


def padded(source, canvas, artwork):
    image = Image.new("RGBA", (canvas, canvas))
    tile = source.resize((artwork, artwork), Image.Resampling.LANCZOS)
    image.alpha_composite(tile, ((canvas - artwork) // 2,) * 2)
    return image


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path, icc_profile=PROFILE, optimize=True)


def main():
    source = Image.open(SOURCE).convert("RGBA")
    # The adaptive canvas is 108dp; Android's nominal mask is 72dp.
    # 56dp artwork keeps the lettering inside the 66dp safe circle.
    for density, scale in [("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2),
                           ("xxhdpi", 3), ("xxxhdpi", 4)]:
        legacy = padded(source, round(48 * scale), round((112 / 3) * scale))
        for name in ("ic_launcher", "ic_launcher_round"):
            save(legacy, RES / f"mipmap-{density}/{name}.png")
        save(padded(source, round(108 * scale), round(56 * scale)),
             RES / f"drawable-{density}/ic_launcher_foreground.png")
        # Android 12+ splash: 288dp canvas, 192dp circular mask.
        save(padded(source, round(288 * scale), round((448 / 3) * scale)),
             RES / f"drawable-{density}/ic_splash.png")
    # Play is a separate opaque, full-square asset; Android retains alpha.
    play = Image.new("RGBA", (512, 512), "#0739A5")
    play.alpha_composite(source.resize((512, 512), Image.Resampling.LANCZOS))
    save(play, DESIGN / "google-play-icon-512.png")
    # Large previews of Android's circular mask and its smaller safe circle.
    foreground = padded(source, 1080, 560)
    for diameter, name in [(720, "circular-mask"), (660, "safe-circle")]:
        mask = Image.new("L", (1080, 1080))
        edge = (1080 - diameter) // 2
        ImageDraw.Draw(mask).ellipse((edge, edge, 1079-edge, 1079-edge), fill=255)
        preview = Image.new("RGBA", foreground.size)
        preview.paste(foreground, mask=mask)
        save(preview, DESIGN / f"preview-{name}.png")
    (DESIGN / "source.json").write_text(json.dumps({
        "source_zip": "FrenchVerbs-final-bevel-icon.zip",
        "master_sha256": hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        "adaptive_artwork_dp": 56, "adaptive_canvas_dp": 108,
        "legacy_artwork_dp": 112 / 3, "legacy_canvas_dp": 48,
        "splash_artwork_dp": 448 / 3, "splash_canvas_dp": 288,
        "play_background": "#0739A5"
    }, indent=2) + "\n")


if __name__ == "__main__":
    main()
