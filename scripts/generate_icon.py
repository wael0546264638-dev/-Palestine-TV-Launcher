#!/usr/bin/env python3
"""Generate high-resolution launcher icons for all mipmap densities."""
import os
import math
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

# Colors matching the new design
BG_DARK = (11, 19, 43, 255)        # #0B132B - Deep navy night
BG_MID = (8, 16, 30, 255)          # #08101E
BG_LIGHT = (5, 8, 17, 255)         # #050811
ACCENT_GOLD = (251, 191, 36, 255)  # #FBBF24
ACCENT_TURQUOISE = (6, 214, 160, 255)  # #06D6A0
WHITE = (255, 255, 255, 255)
TEXT_GOLD = (251, 191, 36, 255)
TEXT_TURQUOISE = (6, 214, 160, 255)


def draw_rounded_rect(draw, bbox, radius, fill=None, outline=None, width=1):
    """Draw a rounded rectangle."""
    x1, y1, x2, y2 = bbox
    draw.rounded_rectangle(bbox, radius=radius, fill=fill, outline=outline, width=width)


def draw_crescent(draw, cx, cy, r, color, bg_color):
    """Draw a crescent moon."""
    # Outer circle
    draw.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color)
    # Inner cutout (offset to create crescent)
    cut_r = int(r * 0.72)
    cut_x = cx + int(r * 0.38)
    draw.ellipse([cut_x - cut_r, cy - cut_r, cut_x + cut_r, cy + cut_r], fill=bg_color)


def draw_play_triangle(draw, cx, cy, size, color):
    """Draw a play triangle pointing right."""
    half = size // 2
    draw.polygon([
        (cx - half, cy - half),
        (cx - half, cy + half),
        (cx + half, cy)
    ], fill=color)


def draw_broadcast_waves(draw, cx, cy, color, max_radius, wave_count=3):
    """Draw broadcast wave arcs."""
    for i in range(wave_count):
        radius = max_radius - i * 8
        if radius > 4:
            # Draw arc from left to right (broadcasting outward)
            start_angle = 150
            end_angle = 30
            # Use arc approximation with polygon
            points = []
            for angle_deg in range(start_angle, end_angle + 1, 5):
                angle = math.radians(angle_deg)
                x = cx + int(radius * math.cos(angle))
                y = cy + int(radius * math.sin(angle))
                points.append((x, y))
            if len(points) > 1:
                draw.line(points, fill=color, width=max(2, max_radius // 20), joint="curve")


def generate_icon(size, path):
    img = Image.new("RGBA", (size, size), BG_DARK)
    draw = ImageDraw.Draw(img)

    # Background gradient effect (layered rounded rects) - deep night gradient
    pad = int(size * 0.06)
    for i in range(4):
        inset = pad + i * int(size * 0.025)
        alpha = 30 + i * 25
        color = (
            BG_DARK[0] + i * 2,
            BG_DARK[1] + i * 2,
            BG_DARK[2] + i * 3,
            alpha
        )
        draw_rounded_rect(draw, [inset, inset, size - inset, size - inset],
                          radius=int(size * 0.14), fill=color)

    # TV Frame - Golden with soft corners (centered)
    frame_margin = int(size * 0.18)
    frame_radius = int(size * 0.055)
    frame_box = [frame_margin, frame_margin, size - frame_margin, size - frame_margin]

    # Outer frame (gold)
    draw_rounded_rect(draw, frame_box, radius=frame_radius, fill=ACCENT_GOLD)

    # Inner screen (deep navy)
    inner_margin = frame_margin + int(size * 0.025)
    inner_radius = int(size * 0.035)
    inner_box = [inner_margin, inner_margin, size - inner_margin, size - inner_margin]
    draw_rounded_rect(draw, inner_box, radius=inner_radius, fill=BG_DARK)

    # Crescent inside TV screen
    cx = size // 2
    cy = size // 2 - int(size * 0.02)
    crescent_r = int(size * 0.085)
    draw_crescent(draw, cx, cy, crescent_r, ACCENT_GOLD, BG_DARK)

    # Play triangle inside crescent
    play_size = int(size * 0.055)
    play_cx = cx + int(size * 0.025)
    play_cy = cy + int(size * 0.015)
    draw_play_triangle(draw, play_cx, play_cy, play_size, BG_DARK)

    # Broadcast waves (turquoise) emanating from TV frame
    wave_cx = size // 2
    wave_cy = size // 2 + int(size * 0.18)
    draw_broadcast_waves(draw, wave_cx, wave_cy, ACCENT_TURQUOISE,
                         max_radius=int(size * 0.18), wave_count=3)

    # Subtle border glow
    border_pad = int(size * 0.03)
    draw_rounded_rect(draw, [border_pad, border_pad, size - border_pad, size - border_pad],
                      radius=int(size * 0.16), outline=ACCENT_TURQUOISE, width=max(1, int(size * 0.015)))

    img.save(path, "PNG", optimize=True)
    print(f"Generated: {path} ({size}x{size})")


def generate_round_icon(size, path):
    """Generate round icon with circular mask."""
    img = Image.new("RGBA", (size, size), BG_DARK)
    draw = ImageDraw.Draw(img)

    # Same content as generate_icon but circular
    pad = int(size * 0.06)
    for i in range(4):
        inset = pad + i * int(size * 0.025)
        alpha = 30 + i * 25
        color = (
            BG_DARK[0] + i * 2,
            BG_DARK[1] + i * 2,
            BG_DARK[2] + i * 3,
            alpha
        )
        draw_rounded_rect(draw, [inset, inset, size - inset, size - inset],
                          radius=int(size * 0.14), fill=color)

    frame_margin = int(size * 0.18)
    frame_radius = int(size * 0.055)
    frame_box = [frame_margin, frame_margin, size - frame_margin, size - frame_margin]
    draw_rounded_rect(draw, frame_box, radius=frame_radius, fill=ACCENT_GOLD)

    inner_margin = frame_margin + int(size * 0.025)
    inner_radius = int(size * 0.035)
    inner_box = [inner_margin, inner_margin, size - inner_margin, size - inner_margin]
    draw_rounded_rect(draw, inner_box, radius=inner_radius, fill=BG_DARK)

    cx = size // 2
    cy = size // 2 - int(size * 0.02)
    crescent_r = int(size * 0.085)
    draw_crescent(draw, cx, cy, crescent_r, ACCENT_GOLD, BG_DARK)

    play_size = int(size * 0.055)
    play_cx = cx + int(size * 0.025)
    play_cy = cy + int(size * 0.015)
    draw_play_triangle(draw, play_cx, play_cy, play_size, BG_DARK)

    wave_cx = size // 2
    wave_cy = size // 2 + int(size * 0.18)
    draw_broadcast_waves(draw, wave_cx, wave_cy, ACCENT_TURQUOISE,
                         max_radius=int(size * 0.18), wave_count=3)

    border_pad = int(size * 0.03)
    draw_rounded_rect(draw, [border_pad, border_pad, size - border_pad, size - border_pad],
                      radius=int(size * 0.16), outline=ACCENT_TURQUOISE, width=max(1, int(size * 0.015)))

    # Apply circular mask
    mask = Image.new("L", (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.ellipse([0, 0, size - 1, size - 1], fill=255)
    img.putalpha(mask)
    img.save(path, "PNG", optimize=True)
    print(f"Generated round: {path}")


def generate_banner(width, height, path):
    """Generate a raster TV banner (Leanback launchers cannot render vector banners)."""
    img = Image.new("RGBA", (width, height), BG_DARK)
    draw = ImageDraw.Draw(img)

    sx = width / 320.0
    sy = height / 180.0

    def P(x, y):
        return (x * sx, y * sy)

    # Deep night gradient background
    for y in range(height):
        ratio = y / height
        r = int(BG_DARK[0] + (BG_MID[0] - BG_DARK[0]) * ratio)
        g = int(BG_DARK[1] + (BG_MID[1] - BG_DARK[1]) * ratio)
        b = int(BG_DARK[2] + (BG_MID[2] - BG_DARK[2]) * ratio)
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))

    # TV Frame in center (golden with soft corners) - matching icon proportions
    # Icon has frame at 18% margin, so banner should be similar relative to min dimension
    min_dim = min(width, height)
    frame_margin = int(min_dim * 0.18)
    frame_radius = int(min_dim * 0.055)
    frame_w = width - 2 * frame_margin
    frame_h = height - 2 * frame_margin
    # Keep TV aspect ratio (16:9-ish like the icon)
    tv_aspect = 16 / 9
    if frame_w / frame_h > tv_aspect:
        frame_w = int(frame_h * tv_aspect)
    else:
        frame_h = int(frame_w / tv_aspect)
    tv_x = (width - frame_w) // 2
    tv_y = (height - frame_h) // 2

    # Outer gold frame
    draw_rounded_rect(draw, [tv_x, tv_y, tv_x + frame_w, tv_y + frame_h],
                      radius=frame_radius, fill=ACCENT_GOLD)

    # Inner screen
    inner_pad = int(min_dim * 0.025)
    draw_rounded_rect(draw,
                      [tv_x + inner_pad, tv_y + inner_pad,
                       tv_x + frame_w - inner_pad, tv_y + frame_h - inner_pad],
                      radius=max(2, frame_radius - inner_pad), fill=BG_DARK)

    # Crescent inside TV
    crescent_cx = tv_x + frame_w // 2
    crescent_cy = tv_y + frame_h // 2 - int(min_dim * 0.02)
    crescent_r = int(min_dim * 0.085)
    draw_crescent(draw, crescent_cx, crescent_cy, crescent_r, ACCENT_GOLD, BG_DARK)

    # Play triangle
    play_size = int(min_dim * 0.055)
    play_cx = crescent_cx + int(min_dim * 0.025)
    play_cy = crescent_cy + int(min_dim * 0.015)
    draw_play_triangle(draw, play_cx, play_cy, play_size, BG_DARK)

    # Broadcast waves
    wave_cx = width // 2
    wave_cy = tv_y + frame_h + int(min_dim * 0.18)
    draw_broadcast_waves(draw, wave_cx, wave_cy, ACCENT_TURQUOISE,
                         max_radius=int(min_dim * 0.18), wave_count=3)

    # Subtle glow border
    border_pad = int(min_dim * 0.03)
    draw_rounded_rect(draw, [border_pad, border_pad, width - border_pad, height - border_pad],
                      radius=int(min_dim * 0.16), outline=ACCENT_TURQUOISE, width=max(2, int(1.5 * min(sx, sy))))

    img.save(path, "PNG", optimize=True)
    print(f"Generated banner: {path} ({width}x{height})")


def main():
    for folder, size in DENSITIES:
        path = os.path.join(BASE_DIR, folder)
        os.makedirs(path, exist_ok=True)

        # Standard square icon
        icon_path = os.path.join(path, "ic_launcher.png")
        generate_icon(size, icon_path)

        # Round icon
        icon_round_path = os.path.join(path, "ic_launcher_round.png")
        generate_round_icon(size, icon_round_path)

    # Raster TV banner (PNG). Vector banners are not rendered by the Leanback launcher.
    banner_dir = os.path.join(BASE_DIR, "drawable-nodpi")
    os.makedirs(banner_dir, exist_ok=True)
    generate_banner(640, 360, os.path.join(banner_dir, "ic_banner.png"))

    # Also generate a larger banner for high-res TV
    generate_banner(1280, 720, os.path.join(banner_dir, "ic_banner_hd.png"))

    print("\nAll icons and banners generated successfully!")
    print("Adaptive launcher icons live in mipmap-anydpi-v26/ic_launcher.xml")
    print("and ic_launcher_round.xml (background + foreground vectors).")
    print("The PNGs above are the fallback for API < 26.")


if __name__ == "__main__":
    main()