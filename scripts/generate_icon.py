#!/usr/bin/env python3
"""Generate high-resolution launcher icons for all mipmap densities."""
import os
from PIL import Image, ImageDraw, ImageFont

BASE_DIR = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res")

# Density: (folder name, pixel size)
DENSITIES = [
    ("mipmap-mdpi", 48),
    ("mipmap-hdpi", 72),
    ("mipmap-xhdpi", 96),
    ("mipmap-xxhdpi", 144),
    ("mipmap-xxxhdpi", 192),
]

# Colors matching the app theme
BG_DARK = (15, 23, 42, 255)        # #0F172A
BG_MID = (30, 41, 59, 255)         # #1E293B
ACCENT_BLUE = (56, 189, 248, 255)  # #38BDF8
ACCENT_GOLD = (251, 191, 36, 255)  # #FBBF24
WHITE = (255, 255, 255, 255)
PALESTINE_GREEN = (22, 163, 74, 255)  # #16A34A
PALESTINE_RED = (220, 38, 38, 255)    # #DC2626
PALESTINE_BLACK = (15, 23, 42, 255)


def draw_flag_stripe(draw, x, y, w, h, color):
    draw.rectangle([x, y, x + w, y + h], fill=color)


def draw_crescent(draw, cx, cy, r, color):
    # Outer circle
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)
    # Inner cutout (offset to create crescent)
    cut_r = int(r * 0.78)
    cut_x = cx + int(r * 0.35)
    draw.ellipse([cut_x - cut_r, cy - cut_r, cut_x + cut_r, cy + cut_r], fill=BG_DARK)


def draw_star(draw, cx, cy, outer_r, color, points=5):
    import math
    coords = []
    for i in range(points * 2):
        angle = math.pi / 2 + (math.pi * i / points)
        r = outer_r if i % 2 == 0 else int(outer_r * 0.4)
        x = cx + int(r * math.cos(angle))
        y = cy - int(r * math.sin(angle))
        coords.append((x, y))
    draw.polygon(coords, fill=color)


def generate_icon(size, path):
    img = Image.new("RGBA", (size, size), BG_DARK)
    draw = ImageDraw.Draw(img)

    # Background gradient effect (layered rounded rects)
    pad = int(size * 0.06)
    for i in range(3):
        inset = pad + i * int(size * 0.02)
        alpha = 40 + i * 20
        draw.rounded_rectangle(
            [inset, inset, size - inset, size - inset],
            radius=int(size * 0.14),
            fill=(30 + i * 8, 41 + i * 8, 59 + i * 8, alpha)
        )

    # Palestine flag stripes (top black, middle white, bottom green)
    flag_top = int(size * 0.18)
    flag_bottom = int(size * 0.82)
    flag_h = flag_bottom - flag_top
    stripe_h = flag_h // 3
    flag_left = int(size * 0.12)
    flag_right = int(size * 0.88)

    # Top stripe (black)
    draw_flag_stripe(draw, flag_left, flag_top, flag_right - flag_left, stripe_h, PALESTINE_BLACK)
    # Middle stripe (white)
    draw_flag_stripe(draw, flag_left, flag_top + stripe_h, flag_right - flag_left, stripe_h, WHITE)
    # Bottom stripe (green)
    draw_flag_stripe(draw, flag_left, flag_top + stripe_h * 2, flag_right - flag_left, flag_h - stripe_h * 2, PALESTINE_GREEN)

    # Red triangle on the left
    tri_x = flag_left
    tri_top = flag_top
    tri_bottom = flag_bottom
    tri_right = flag_left + int((flag_right - flag_left) * 0.28)
    draw.polygon(
        [(tri_x, tri_top), (tri_x, tri_bottom), (tri_right, (tri_top + tri_bottom) // 2)],
        fill=PALESTINE_RED
    )

    # Crescent and star in the center-right of the flag
    crescent_cx = int(size * 0.62)
    crescent_cy = size // 2
    crescent_r = int(size * 0.09)
    draw_crescent(draw, crescent_cx, crescent_cy, crescent_r, WHITE)
    draw_star(draw, crescent_cx + int(size * 0.07), crescent_cy, int(size * 0.04), WHITE)

    # Border glow
    border_pad = int(size * 0.04)
    draw.rounded_rectangle(
        [border_pad, border_pad, size - border_pad, size - border_pad],
        radius=int(size * 0.16),
        outline=ACCENT_BLUE,
        width=max(2, int(size * 0.03))
    )

    # TV screen hint (small play triangle at bottom)
    play_size = int(size * 0.06)
    play_cx = size // 2
    play_cy = int(size * 0.88)
    draw.polygon(
        [(play_cx - play_size // 2, play_cy - play_size // 2),
         (play_cx - play_size // 2, play_cy + play_size // 2),
         (play_cx + play_size // 2, play_cy)],
        fill=ACCENT_GOLD
    )

    img.save(path, "PNG", optimize=True)
    print(f"Generated: {path} ({size}x{size})")


def generate_banner(width, height, path):
    """Generate a raster TV banner (Leanback launchers cannot render vector banners)."""
    img = Image.new("RGBA", (width, height), BG_DARK)
    draw = ImageDraw.Draw(img)
    sx = width / 320.0
    sy = height / 180.0

    def P(x, y):
        return (x * sx, y * sy)

    # Subtle grid
    grid = (30, 58, 95, 255)
    for gx in (95, 225):
        draw.line([P(gx, 0), P(gx, 180)], fill=grid, width=1)
    for gy in (45, 135):
        draw.line([P(0, gy), P(320, gy)], fill=grid, width=1)

    # Hexagon outline
    hex_pts = [P(160, 32), P(196, 53), P(196, 95), P(160, 116), P(124, 95), P(124, 53)]
    draw.line(hex_pts + [hex_pts[0]], fill=ACCENT_BLUE, width=max(2, int(1.5 * sx)))

    # Play triangle
    draw.polygon([P(148, 54), P(182, 74), P(148, 94)], fill=ACCENT_GOLD)

    # Sparkle
    draw.polygon([P(166, 64), P(168, 70), P(174, 72), P(168, 74),
                  P(166, 80), P(164, 74), P(158, 72), P(164, 70)], fill=ACCENT_BLUE)

    # Lower bar
    draw.rounded_rectangle([P(85, 134), P(235, 166)], radius=int(8 * sy), fill=BG_MID)
    draw.rectangle([P(115, 146), P(205, 154)], fill=ACCENT_GOLD)

    img.save(path, "PNG", optimize=True)
    print(f"Generated banner: {path} ({width}x{height})")


def main():
    for folder, size in DENSITIES:
        path = os.path.join(BASE_DIR, folder)
        os.makedirs(path, exist_ok=True)
        icon_path = os.path.join(path, "ic_launcher.png")
        icon_round_path = os.path.join(path, "ic_launcher_round.png")
        generate_icon(size, icon_path)
        # Round icon: same image, circular mask
        img = Image.open(icon_path).convert("RGBA")
        mask = Image.new("L", (size, size), 0)
        mask_draw = ImageDraw.Draw(mask)
        mask_draw.ellipse([0, 0, size - 1, size - 1], fill=255)
        img.putalpha(mask)
        img.save(icon_round_path, "PNG", optimize=True)
        print(f"Generated round: {icon_round_path}")

    # Raster TV banner (PNG). Vector banners are not rendered by the Leanback launcher.
    banner_dir = os.path.join(BASE_DIR, "drawable-nodpi")
    os.makedirs(banner_dir, exist_ok=True)
    generate_banner(640, 360, os.path.join(banner_dir, "ic_banner.png"))

    # Adaptive launcher icons live in mipmap-anydpi-v26/ic_launcher.xml and
    # ic_launcher_round.xml (background + foreground vectors). The PNGs above are
    # the fallback for API < 26.

if __name__ == "__main__":
    main()
