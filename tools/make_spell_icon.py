#!/usr/bin/env python3
"""把原画转成 mod 里用的法术 / 效果图标。

规格（对齐 mana_recharge.png 那一批）
------------------------------------
    法术图标：114x103，PNG，不透明
    效果图标：16x16，PNG

原图通常是 1254x1254 的正方图，所以流程是：

    等比缩放到宽 114  ->  114 x 114
    垂直居中裁到 103  ->  114 x 103

也就是说上下各切掉约 5px，**不做非等比拉伸**，人物不会被压扁。

缩小方式（--resample，默认 mode）
--------------------------------
    mode     主色取样：每个输出像素取对应源区块里「占面积最大」的颜色，不插值、不混色。
             像素画的色块边界保持硬边，是这批图最合适的方式。
    nearest  最近邻：最锐，但会丢细节（细高光、小火花可能整块消失）。
    box      盒式平均：会混色，缩完发糊 —— 只在要柔和过渡时用。
    lanczos  高质量平滑：最糊，基本不用。

用法
----
    # 最常用：输出到 spell_icons，文件名给技能 id
    python tools/make_spell_icon.py 标记.png -n weak_point_mark

    # 批量（名字数量要和输入一致）
    python tools/make_spell_icon.py 图1.png 图2.png 图3.png -n a b c

    # 生成 HUD 效果小图标（16x16，取画面正中方形）
    python tools/make_spell_icon.py 图.png -n foo --kind effect

    # 换缩小方式（默认 mode=主色取样，不发糊）
    python tools/make_spell_icon.py 图.png -n foo --resample nearest

    # 想先裁掉原图外圈再缩放时（四边各裁多少像素）
    python tools/make_spell_icon.py 图.png -n foo --trim 16

    # 输出放大预览，肉眼检查（.workbuddy/preview_<name>.png）
    python tools/make_spell_icon.py 图.png -n foo --preview
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from PIL import Image

#: 法术图标成品尺寸，对齐既有素材（mana_recharge / taunt / power_shot ...）
SPELL_SIZE = (114, 103)
#: HUD 效果图标边长
EFFECT_SIZE = 16

ASSETS = Path("src/main/resources/assets/fast_irons_spellbooks_addition/textures")
SPELL_DIR = ASSETS / "gui/spell_icons"
EFFECT_DIR = ASSETS / "mob_effect"


def find_project_root(start: Path) -> Path:
    """从脚本位置往上找带 src/main/resources 的目录，不依赖当前工作目录。"""
    for candidate in [start, *start.parents]:
        if (candidate / "src/main/resources").is_dir():
            return candidate
    raise SystemExit("找不到项目根目录（缺少 src/main/resources）")


def load_image(path: Path) -> Image.Image:
    image = Image.open(path)
    return image if image.mode == "RGBA" else image.convert("RGBA")


def trim_edges(image: Image.Image, pixels: int) -> Image.Image:
    """四边各裁掉若干像素，用来去掉原画自带的细边框。"""
    if pixels <= 0:
        return image
    width, height = image.size
    box = (pixels, pixels, width - pixels, height - pixels)
    if box[2] - box[0] < 8 or box[3] - box[1] < 8:
        raise SystemExit(f"--trim {pixels} 太大，裁完只剩 {box[2] - box[0]}x{box[3] - box[1]}")
    return image.crop(box)


#: 可选的缩小方式
RESAMPLE_CHOICES = ("mode", "nearest", "box", "lanczos")


def dominant_resize(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    """主色取样缩小：每个输出像素取源区块里占比最大的颜色，不插值。

    像素画缩小发糊的根源是「平均」——一个 11x11 的源块里同时有深蓝底和白色高光，
    平均出来就是一片灰蓝。这里改成先按 5 bit/通道量化统计出**主色**，
    再取该主色桶内像素的均值当输出色：色块边界保持硬边，同色区域也不会带噪点。
    """
    source = image.convert("RGBA")
    src_pixels = source.load()
    src_w, src_h = source.size
    dst_w, dst_h = size

    result = Image.new("RGBA", size, (0, 0, 0, 0))
    dst_pixels = result.load()

    for dy in range(dst_h):
        y0 = dy * src_h // dst_h
        y1 = max(y0 + 1, (dy + 1) * src_h // dst_h)
        for dx in range(dst_w):
            x0 = dx * src_w // dst_w
            x1 = max(x0 + 1, (dx + 1) * src_w // dst_w)

            # key -> [像素数, r和, g和, b和, a和]
            buckets: dict[int, list[int]] = {}
            for sy in range(y0, y1):
                for sx in range(x0, x1):
                    r, g, b, a = src_pixels[sx, sy]
                    if a < 32:
                        continue
                    key = ((r >> 3) << 10) | ((g >> 3) << 5) | (b >> 3)
                    bucket = buckets.get(key)
                    if bucket is None:
                        buckets[key] = [1, r, g, b, a]
                    else:
                        bucket[0] += 1
                        bucket[1] += r
                        bucket[2] += g
                        bucket[3] += b
                        bucket[4] += a

            if not buckets:
                continue
            count, sr, sg, sb, sa = max(buckets.values(), key=lambda it: it[0])
            dst_pixels[dx, dy] = (
                sr // count, sg // count, sb // count, sa // count,
            )

    return result


def resize(image: Image.Image, size: tuple[int, int], method: str) -> Image.Image:
    """按指定方式缩小。"""
    if method == "mode":
        return dominant_resize(image, size)
    resample = {
        "nearest": Image.Resampling.NEAREST,
        "box": Image.Resampling.BOX,
        "lanczos": Image.Resampling.LANCZOS,
    }[method]
    return image.resize(size, resample)


def fit_spell(image: Image.Image, size: tuple[int, int], method: str = "mode") -> Image.Image:
    """等比缩放到目标宽度，再垂直居中裁到目标高度；短了就用透明补。"""
    width, height = size
    scaled_height = max(1, round(image.height * width / image.width))
    scaled = resize(image, (width, scaled_height), method)

    if scaled_height == height:
        return scaled
    if scaled_height > height:
        top = (scaled_height - height) // 2
        return scaled.crop((0, top, width, top + height))

    canvas = Image.new("RGBA", size, (0, 0, 0, 0))
    canvas.paste(scaled, (0, (height - scaled_height) // 2))
    return canvas


def fit_effect(image: Image.Image, size: int, method: str = "mode") -> Image.Image:
    """效果图标：取画面正中方形再缩小，避免构图被压扁。"""
    width, height = image.size
    side = min(width, height)
    left = (width - side) // 2
    top = (height - side) // 2
    square = image.crop((left, top, left + side, top + side))
    return resize(square, (size, size), method)


def write_preview(image: Image.Image, name: str) -> Path:
    """放大铺深灰底，方便肉眼检查。"""
    scale = max(2, 360 // max(image.size))
    canvas = Image.new("RGBA", (image.width * scale, image.height * scale), (34, 36, 42, 255))
    canvas.alpha_composite(image.resize((image.width * scale, image.height * scale), Image.Resampling.NEAREST))
    out = Path(".workbuddy") / f"preview_{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(out)
    return out


def main() -> int:
    parser = argparse.ArgumentParser(
        description="把原画转成 mod 内的法术 / 效果图标（法术 114x103，效果 16x16）",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("inputs", nargs="+", help="原图路径，可多个")
    parser.add_argument("-n", "--names", nargs="+", help="输出文件名（不含 .png），数量要和输入一致")
    parser.add_argument("--kind", choices=["spell", "effect"], default="spell",
                        help="spell=法术图标 114x103（默认），effect=HUD 效果图标 16x16")
    parser.add_argument("--trim", type=int, default=0,
                        help="缩放前四边各裁掉多少像素，默认 0（不裁）")
    parser.add_argument("--resample", choices=RESAMPLE_CHOICES, default="mode",
                        help="缩小方式：mode=主色取样（默认，不发糊）/ nearest=最近邻 / box=平均 / lanczos=平滑")
    parser.add_argument("--preview", action="store_true", help="额外输出放大预览图")
    args = parser.parse_args()

    root = find_project_root(Path(__file__).resolve().parent)
    out_dir = root / (SPELL_DIR if args.kind == "spell" else EFFECT_DIR)
    out_dir.mkdir(parents=True, exist_ok=True)

    names = args.names or [Path(p).stem for p in args.inputs]
    if len(names) != len(args.inputs):
        print(f"输入 {len(args.inputs)} 个，名字 {len(names)} 个，对不上", file=sys.stderr)
        return 2

    for src, name in zip(args.inputs, names):
        src_path = Path(src)
        if not src_path.is_file():
            print(f"跳过（不存在）：{src_path}", file=sys.stderr)
            continue

        image = trim_edges(load_image(src_path), args.trim)
        result = (fit_spell(image, SPELL_SIZE, args.resample) if args.kind == "spell"
                  else fit_effect(image, EFFECT_SIZE, args.resample))

        dst = out_dir / f"{name}.png"
        result.save(dst)
        print(f"{src_path.name}  {image.size} -> {dst.relative_to(root)}  {result.size}  [{args.resample}]")

        if args.preview:
            print(f"    预览：{write_preview(result, name)}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
