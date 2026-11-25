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
BG_DARK = (10, 15, 30, 255)        # #0A0F1E - Deep navy night
BG_MID = (15, 23, 42, 255)         # #0F172A
BG_LIGHT = (20, 30, 58, 255)       # #141E3A
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

    # Background gradient effect (layered rounded rects)
    pad = int(size * 0.06)
    for i in range(4):
        inset = pad + i * int(size * 0.025)
        alpha = 30 + i * 25
        color = (
            BG_DARK[0] + i * 3,
            BG_DARK[1] + i * 3,
            BG_DARK[2] + i * 5,
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

    # Inner screen (dark navy)
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

    # Text: "Palestine TV" in Gold (below waves)
    try:
        font_size_large = max(10, int(size * 0.075))
        font_size_small = max(8, int(size * 0.055))
        font_large = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", font_size_large)
        font_small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", font_size_small)
    except:
        font_large = ImageFont.load_default()
        font_small = ImageFont.load_default()

    text_y = wave_cy + int(size * 0.14)
    # "Palestine TV"
    text1 = "Palestine TV"
    bbox1 = draw.textbbox((0, 0), text1, font=font_large)
    text1_w = bbox1[2] - bbox1[0]
    draw.text(((size - text1_w) // 2, text_y), text1, font=font_large, fill=TEXT_GOLD)

    # "Adaptive Launcher" in Turquoise
    text_y2 = text_y + (bbox1[3] - bbox1[1]) + int(size * 0.015)
    text2 = "Adaptive Launcher"
    bbox2 = draw.textbbox((0, 0), text2, font=font_small)
    text2_w = bbox2[2] - bbox2[0]
    draw.text(((size - text2_w) // 2, text_y2), text2, font=font_small, fill=TEXT_TURQUOISE)

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
            BG_DARK[0] + i * 3,
            BG_DARK[1] + i * 3,
            BG_DARK[2] + i * 5,
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

    try:
        font_size_large = max(10, int(size * 0.075))
        font_size_small = max(8, int(size * 0.055))
        font_large = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", font_size_large)
        font_small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", font_size_small)
    except:
        font_large = ImageFont.load_default()
        font_small = ImageFont.load_default()

    text_y = wave_cy + int(size * 0.14)
    text1 = "Palestine TV"
    bbox1 = draw.textbbox((0, 0), text1, font=font_large)
    text1_w = bbox1[2] - bbox1[0]
    draw.text(((size - text1_w) // 2, text_y), text1, font=font_large, fill=TEXT_GOLD)

    text_y2 = text_y + (bbox1[3] - bbox1[1]) + int(size * 0.015)
    text2 = "Adaptive Launcher"
    bbox2 = draw.textbbox((0, 0), text2, font=font_small)
    text2_w = bbox2[2] - bbox2[0]
    draw.text(((size - text2_w) // 2, text_y2), text2, font=font_small, fill=TEXT_TURQUOISE)

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

    # Subtle dark gradient background
    for y in range(height):
        ratio = y / height
        r = int(BG_DARK[0] + (BG_MID[0] - BG_DARK[0]) * ratio)
        g = int(BG_DARK[1] + (BG_MID[1] - BG_DARK[1]) * ratio)
        b = int(BG_DARK[2] + (BG_MID[2] - BG_DARK[2]) * ratio)
        draw.line([(0, y), (width, y)], fill=(r, g, b, 255))

    # Subtle grid pattern
    grid_color = (30, 58, 95, 40)
    for gx in range(0, width, int(65 * sx)):
        draw.line([(gx, 0), (gx, height)], fill=grid_color, width=1)
    for gy in range(0, height, int(45 * sy)):
        draw.line([(0, gy), (width, gy)], fill=grid_color, width=1)

    # TV Frame in center (golden with soft corners)
    tv_w = int(160 * sx)
    tv_h = int(100 * sy)
    tv_x = (width - tv_w) // 2
    tv_y = (height - tv_h) // 2 - int(10 * sy)
    tv_radius = int(8 * min(sx, sy))

    # Outer gold frame
    draw_rounded_rect(draw, [tv_x, tv_y, tv_x + tv_w, tv_y + tv_h],
                      radius=tv_radius, fill=ACCENT_GOLD)

    # Inner screen
    inner_pad = int(4 * min(sx, sy))
    draw_rounded_rect(draw,
                      [tv_x + inner_pad, tv_y + inner_pad,
                       tv_x + tv_w - inner_pad, tv_y + tv_h - inner_pad],
                      radius=max(2, tv_radius - inner_pad), fill=BG_DARK)

    # Crescent inside TV
    crescent_cx = tv_x + tv_w // 2
    crescent_cy = tv_y + tv_h // 2 - int(3 * sy)
    crescent_r = int(14 * min(sx, sy))
    draw_crescent(draw, crescent_cx, crescent_cy, crescent_r, ACCENT_GOLD, BG_DARK)

    # Play triangle
    play_size = int(10 * min(sx, sy))
    play_cx = crescent_cx + int(4 * sx)
    play_cy = crescent_cy + int(2 * sy)
    draw_play_triangle(draw, play_cx, play_cy, play_size, BG_DARK)

    # Broadcast waves
    wave_cx = width // 2
    wave_cy = tv_y + tv_h + int(20 * sy)
    draw_broadcast_waves(draw, wave_cx, wave_cy, ACCENT_TURQUOISE,
                         max_radius=int(50 * min(sx, sy)), wave_count=4)

    # Text: "Palestine TV" in Gold
    try:
        font_size_banner_large = max(18, int(24 * min(sx, sy)))
        font_size_banner_small = max(14, int(16 * min(sx, sy)))
        font_banner_large = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", font_size_banner_large)
        font_banner_small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", font_size_banner_small)
    except:
        font_banner_large = ImageFont.load_default()
        font_banner_small = ImageFont.load_default()

    text_y = wave_cy + int(20 * sy)
    text1 = "Palestine TV"
    bbox1 = draw.textbbox((0, 0), text1, font=font_banner_large)
    text1_w = bbox1[2] - bbox1[0]
    draw.text(((width - text1_w) // 2, text_y), text1, font=font_banner_large, fill=TEXT_GOLD)

    text_y2 = text_y + (bbox1[3] - bbox1[1]) + int(8 * sy)
    text2 = "Adaptive Launcher"
    bbox2 = draw.textbbox((0, 0), text2, font=font_banner_small)
    text2_w = bbox2[2] - bbox2[0]
    draw.text(((width - text2_w) // 2, text_y2), text2, font=font_banner_small, fill=TEXT_TURQUOISE)

    # Subtle glow border
    border_pad = int(8 * min(sx, sy))
    draw_rounded_rect(draw, [border_pad, border_pad, width - border_pad, height - border_pad],
                      radius=int(12 * min(sx, sy)), outline=ACCENT_TURQUOISE, width=max(2, int(1.5 * min(sx, sy))))

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