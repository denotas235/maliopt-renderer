#!/usr/bin/env python3
"""
png_to_astc.py  —  Pipeline de conversão PNG → ASTC para mod Fabric
Converte texturas vanilla do Minecraft (terrain/item) para ASTC 4x4
e empacota em src/main/resources para inclusão no JAR.

Uso:
    python3 png_to_astc.py <minecraft.jar> [--quality fast|medium|thorough]

Requisitos:
    astcenc  (ver install_astcenc.sh)
    Python 3.8+
"""

import os
import sys
import subprocess
import zipfile
import shutil
import struct
import json
import argparse
from pathlib import Path

# ── Configuração ─────────────────────────────────────────────────────────────

BLOCK_SIZE   = "4x4"        # Bloco ASTC: 4x4 = razão 4:1 para RGBA8
OUTPUT_BASE  = Path("src/main/resources/assets/hyengra/textures_astc")
MANIFEST_JSON = Path("astc_manifest.json")  # mapa { "namespace:path" → "astc_path" }

# Prefixos dentro do minecraft.jar a extrair
TARGET_PREFIXES = (
    "assets/minecraft/textures/block/",
    "assets/minecraft/textures/item/",
    "assets/minecraft/textures/environment/",
    "assets/minecraft/textures/entity/",
)

QUALITY_FLAGS = {
    "fast":     "-fast",
    "medium":   "-medium",
    "thorough": "-thorough",   # default: melhor qualidade, mais lento
}

# ── Utilitários ───────────────────────────────────────────────────────────────

def find_astcenc() -> str | None:
    """Procura o binário astcenc em variantes de SIMD."""
    for name in ("astcenc-avx2", "astcenc-sse4.1", "astcenc-sse2", "astcenc", "astcenc-native"):
        if shutil.which(name):
            return name
    return None


def validate_astc_magic(path: Path) -> bool:
    """Verifica magic number do ficheiro .astc (0x5CA1AB13)."""
    try:
        with open(path, "rb") as f:
            magic = struct.unpack("<I", f.read(4))[0]
        return magic == 0x5CA1AB13
    except Exception:
        return False


def read_astc_header(path: Path) -> dict:
    """Lê header do ficheiro .astc e devolve metadados."""
    with open(path, "rb") as f:
        data = f.read(16)
    magic     = struct.unpack("<I", data[0:4])[0]
    block_x   = data[4]
    block_y   = data[5]
    block_z   = data[6]
    width     = data[7]  | (data[8]  << 8) | (data[9]  << 16)
    height    = data[10] | (data[11] << 8) | (data[12] << 16)
    depth     = data[13] | (data[14] << 8) | (data[15] << 16)
    size_kb   = path.stat().st_size / 1024
    return {
        "magic":   hex(magic),
        "block":   f"{block_x}x{block_y}x{block_z}",
        "width":   width,
        "height":  height,
        "depth":   depth,
        "size_kb": round(size_kb, 2),
    }

# ── Extração ──────────────────────────────────────────────────────────────────

def extract_pngs(jar_path: Path, tmp_dir: Path) -> list[Path]:
    """Extrai PNGs alvo do minecraft.jar para tmp_dir."""
    extracted = []
    with zipfile.ZipFile(jar_path, "r") as jar:
        entries = [e for e in jar.namelist()
                   if e.endswith(".png") and any(e.startswith(p) for p in TARGET_PREFIXES)]
        print(f"  {len(entries)} PNGs encontradas no JAR")
        for entry in entries:
            dest = tmp_dir / entry
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes(jar.read(entry))
            extracted.append(dest)
    return extracted


# ── Conversão ─────────────────────────────────────────────────────────────────

def convert_png_to_astc(png: Path, astcenc: str, quality: str) -> Path | None:
    """
    Converte um PNG para ASTC 4x4 usando astcenc.
    Devolve o Path do .astc gerado, ou None em caso de falha.

    astcenc CLI:  astcenc -cl <input.png> <output.astc> <block> <quality>
      -cl  = compress, LDR (Low Dynamic Range)
    """
    astc = png.with_suffix(".astc")
    cmd = [astcenc, "-cl", str(png), str(astc), BLOCK_SIZE, quality]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0 or not astc.exists():
        print(f"    ✗ FAIL {png.name}: {result.stderr.strip()[:120]}")
        return None
    if not validate_astc_magic(astc):
        print(f"    ✗ MAGIC inválido: {astc.name}")
        astc.unlink(missing_ok=True)
        return None
    return astc


# ── Pipeline principal ────────────────────────────────────────────────────────

def run(jar_path: Path, quality_flag: str, dry_run: bool):
    astcenc = find_astcenc()
    if not astcenc:
        print("ERRO: astcenc não encontrado.")
        print("  Execute: bash install_astcenc.sh")
        sys.exit(1)
    print(f"Usando encoder: {astcenc}")

    if not jar_path.exists():
        print(f"ERRO: JAR não encontrado: {jar_path}")
        sys.exit(1)

    tmp = Path("/tmp/mc_astc_pipeline")
    if tmp.exists():
        shutil.rmtree(tmp)
    tmp.mkdir(parents=True)

    # 1. Extracção
    print(f"\n[1/3] Extraindo PNGs de {jar_path.name}...")
    pngs = extract_pngs(jar_path, tmp)

    # 2. Conversão
    print(f"\n[2/3] Convertendo {len(pngs)} texturas → ASTC {BLOCK_SIZE} ({quality_flag})...")
    manifest: dict[str, dict] = {}
    ok = fail = 0
    total_png_kb = total_astc_kb = 0.0

    for i, png in enumerate(sorted(pngs), 1):
        rel   = png.relative_to(tmp)                     # assets/minecraft/textures/block/stone.png
        parts = rel.parts                                 # ('assets','minecraft','textures','block','stone.png')
        ns    = parts[1]                                  # minecraft
        # Chave de manifesto: namespace:textures/block/stone
        tex_path = "/".join(parts[2:]).removesuffix(".png")  # textures/block/stone
        key   = f"{ns}:{tex_path}"

        png_kb = png.stat().st_size / 1024
        total_png_kb += png_kb

        if dry_run:
            print(f"  [DRY] {key}")
            ok += 1
            continue

        astc = convert_png_to_astc(png, astcenc, quality_flag)
        if astc is None:
            fail += 1
            continue

        # Destino no mod: assets/hyengra/textures_astc/<ns>/<tex_path>.astc
        dest = OUTPUT_BASE / ns / (tex_path + ".astc")
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(astc, dest)

        meta = read_astc_header(dest)
        total_astc_kb += meta["size_kb"]
        manifest[key] = {
            "astc": str(dest.relative_to(Path("src/main/resources"))),
            "width": meta["width"],
            "height": meta["height"],
            "block": meta["block"],
            "size_kb": meta["size_kb"],
            "original_kb": round(png_kb, 2),
        }

        if i % 50 == 0 or i == len(pngs):
            print(f"    {i}/{len(pngs)}  ok={ok+1}  fail={fail}")
        ok += 1

    # 3. Manifesto
    print(f"\n[3/3] Gravando manifesto...")
    manifest_dest = Path("src/main/resources/assets/hyengra") / MANIFEST_JSON.name
    manifest_dest.parent.mkdir(parents=True, exist_ok=True)
    with open(manifest_dest, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)

    # Relatório
    ratio = (1 - total_astc_kb / total_png_kb) * 100 if total_png_kb > 0 else 0
    print(f"""
═══════════════════════════════════════
  Conversão concluída
  OK:       {ok}
  FAIL:     {fail}
  PNG total: {total_png_kb/1024:.1f} MB
  ASTC total:{total_astc_kb/1024:.1f} MB
  Redução:  {ratio:.1f}%
  Manifesto: {manifest_dest}
═══════════════════════════════════════
""")

    shutil.rmtree(tmp)


# ── Entry point ───────────────────────────────────────────────────────────────

def main():
    p = argparse.ArgumentParser(description="PNG → ASTC pipeline para mod Fabric")
    p.add_argument("jar",     help="Caminho para minecraft.jar")
    p.add_argument("--quality", choices=QUALITY_FLAGS, default="thorough",
                   help="Qualidade de compressão (default: thorough)")
    p.add_argument("--dry-run", action="store_true",
                   help="Lista texturas sem converter")
    args = p.parse_args()

    run(Path(args.jar), QUALITY_FLAGS[args.quality], args.dry_run)


if __name__ == "__main__":
    main()
