"""把 plan/brand/qingyue-icon.jpg（seedream 生成）整理成 Android 各密度图标。

用法：python3 plan/brand/make_icons.py
依赖：opencv-python、numpy、Pillow
"""
from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT / "plan/brand/qingyue-icon.jpg"
BASE = ROOT / "plan/brand/icon-base-clean.png"
RES = ROOT / "app/src/main/res"

# 1. 去掉四角的白底（flood fill 只吃连通白色区域，不会碰到中间的纸张）
img = cv2.imread(str(SRC))
h, w = img.shape[:2]
near_white = (img.min(axis=2) > 195).astype(np.uint8) * 255
corner = near_white.copy()
mask = np.zeros((h + 2, w + 2), np.uint8)
for seed in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:
    cv2.floodFill(corner, mask, seed, 128)
corner_mask = (corner == 128).astype(np.uint8) * 255
corner_mask = cv2.dilate(corner_mask, np.ones((5, 5), np.uint8), iterations=2)

# 2. 抹掉右下角水印（固定矩形，位于纸张右下角之外）
patch_mask = corner_mask.copy()
patch_mask[1510:1885, 1410:1905] = 255
clean = cv2.inpaint(img, patch_mask, 15, cv2.INPAINT_TELEA)
cv2.imwrite(str(BASE), clean)

base = Image.open(BASE).convert("RGB")


def rounded(im: Image.Image, size: int, radius_ratio: float = 0.22) -> Image.Image:
    out = im.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(mask)
    r = int(size * 4 * radius_ratio)
    d.rounded_rectangle((0, 0, size * 4 - 1, size * 4 - 1), radius=r, fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    out.putalpha(mask)
    return out


def circular(im: Image.Image, size: int) -> Image.Image:
    out = im.resize((size, size), Image.LANCZOS)
    mask = Image.new("L", (size * 4, size * 4), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    mask = mask.resize((size, size), Image.LANCZOS)
    out.putalpha(mask)
    return out


densities = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
fg_densities = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}

for name, px in densities.items():
    d = RES / f"mipmap-{name}"
    d.mkdir(parents=True, exist_ok=True)
    rounded(base, px).save(d / "ic_launcher.webp", lossless=True, quality=100)
    circular(base, px).save(d / "ic_launcher_round.webp", lossless=True, quality=100)
    base.resize((fg_densities[name],) * 2, Image.LANCZOS).save(
        d / "ic_launcher_foreground.webp", lossless=True, quality=100
    )

# 3. 单色图层：只保留中间纸张的白色部分，供 Android 13 主题图标着色
arr = np.array(base)
glyph = (arr.min(axis=2) > 205).astype(np.uint8)
num, labels, stats, _ = cv2.connectedComponentsWithStats(glyph, 8)
largest = max(range(1, num), key=lambda i: stats[i, cv2.CC_STAT_AREA])
x, y, bw, bh = (int(v) for v in stats[largest, :4])
crop = ((labels[y:y + bh, x:x + bw] == largest) * 255).astype(np.uint8)
alpha = Image.fromarray(crop).resize((360, 360), Image.LANCZOS)
mono = Image.new("RGBA", (432, 432), (255, 255, 255, 0))
mono.paste(Image.new("RGBA", (360, 360), (255, 255, 255, 255)), (36, 36), alpha)
for name, px in fg_densities.items():
    d = RES / f"mipmap-{name}"
    mono.resize((px, px), Image.LANCZOS).save(
        d / "ic_launcher_monochrome.webp", lossless=True, quality=100
    )

print("done", BASE, base.size, "glyph box", (x, y, bw, bh))
