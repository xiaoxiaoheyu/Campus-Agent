from pathlib import Path
import sys
from PIL import Image

root = Path(__file__).resolve().parents[1]
default_src = root / "static" / "brand" / "app-icon.png"
cli_src = Path(sys.argv[1]) if len(sys.argv) > 1 else None
src = cli_src if cli_src and cli_src.exists() else default_src
if not src.exists():
    raise SystemExit(f"icon source not found: {src}")

icons_dir = root / "static" / "app-icons"
brand_dir = root / "static" / "brand"
unpackage_icons = root / "unpackage" / "res" / "icons"
for d in (icons_dir, brand_dir, unpackage_icons):
    d.mkdir(parents=True, exist_ok=True)

img = Image.open(src).convert("RGBA")
bg = Image.new("RGB", img.size, (255, 255, 255))
bg.paste(img, mask=img.split()[-1])
master = bg

sizes = [20, 29, 40, 58, 60, 72, 76, 80, 87, 96, 114, 120, 144, 152, 167, 180, 192, 1024]
for size in sizes:
    out = master.resize((size, size), Image.Resampling.LANCZOS)
    name = f"{size}x{size}.png"
    out.save(icons_dir / name, format="PNG", optimize=True)
    out.save(unpackage_icons / name, format="PNG", optimize=True)

master.save(brand_dir / "app-icon.png", format="PNG", optimize=True)
print("source:", src)
print("icons:", sorted(p.name for p in icons_dir.glob("*.png")))
