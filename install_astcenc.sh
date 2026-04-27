#!/usr/bin/env bash
# install_astcenc.sh  —  Instala astcenc e baixa minecraft.jar correto
# Compatível com Ubuntu 22.04+ (Codespace / GitHub Actions)
set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────
MC_VERSION="${MC_VERSION:-1.21.10}"   # override via env se necessário
ASTCENC_VERSION="4.7.0"
ASTCENC_URL="https://github.com/ARM-software/astc-encoder/releases/download/${ASTCENC_VERSION}/astcenc-${ASTCENC_VERSION}-linux-x64.zip"
INSTALL_DIR="/usr/local/bin"

echo "════════════════════════════════════════"
echo " ASTC Pipeline — Setup"
echo " astcenc ${ASTCENC_VERSION}  |  MC ${MC_VERSION}"
echo "════════════════════════════════════════"

# ── 1. Instalar dependências Python ──────────────────────────────────────────
echo ""
echo "[1/3] Dependências Python..."
pip install --quiet requests

# ── 2. Instalar astcenc ───────────────────────────────────────────────────────
echo ""
echo "[2/3] Instalando astcenc..."

if command -v astcenc-avx2 &>/dev/null; then
    echo "  astcenc já instalado: $(astcenc-avx2 --version 2>&1 | head -1)"
else
    TMP_ZIP="/tmp/astcenc.zip"
    echo "  Baixando ${ASTCENC_URL}..."
    curl -fsSL -o "${TMP_ZIP}" "${ASTCENC_URL}"
    
    TMP_DIR="/tmp/astcenc_extract"
    mkdir -p "${TMP_DIR}"
    unzip -q -o "${TMP_ZIP}" -d "${TMP_DIR}"
    
    # Copiar todos os binários para /usr/local/bin
    find "${TMP_DIR}" -name "astcenc*" -type f | while read -r bin; do
        chmod +x "${bin}"
        sudo cp "${bin}" "${INSTALL_DIR}/"
        echo "  Instalado: $(basename ${bin})"
    done
    
    rm -rf "${TMP_ZIP}" "${TMP_DIR}"
    
    # Verificar
    if command -v astcenc-avx2 &>/dev/null; then
        echo "  ✓ astcenc-avx2 OK"
    elif command -v astcenc &>/dev/null; then
        echo "  ✓ astcenc OK (sem AVX2)"
    else
        echo "  ✗ ERRO: astcenc não encontrado após instalação"
        exit 1
    fi
fi

# ── 3. Baixar minecraft.jar ───────────────────────────────────────────────────
echo ""
echo "[3/3] Baixando minecraft.jar ${MC_VERSION}..."

JAR_TARGET="minecraft-${MC_VERSION}.jar"

if [ -f "${JAR_TARGET}" ]; then
    echo "  JAR já existe: ${JAR_TARGET}"
else
    MANIFEST_URL="https://launchermeta.mojang.com/mc/game/version_manifest_v2.json"
    echo "  Buscando manifesto Mojang..."
    
    # Extrai URL do manifesto da versão específica
    VERSION_URL=$(python3 - <<EOF
import urllib.request, json, sys
with urllib.request.urlopen("${MANIFEST_URL}") as r:
    manifest = json.load(r)
target = "${MC_VERSION}"
for v in manifest["versions"]:
    if v["id"] == target:
        print(v["url"])
        sys.exit(0)
# Tenta correspondência parcial (ex: "1.21" → "1.21.10")
for v in manifest["versions"]:
    if v["id"].startswith(target):
        print(v["url"], file=sys.stderr)
        print(f"AVISO: versão exata '{target}' não encontrada, usando '{v['id']}'", file=sys.stderr)
        print(v["url"])
        sys.exit(0)
print(f"ERRO: versão '{target}' não encontrada no manifesto", file=sys.stderr)
sys.exit(1)
EOF
)

    if [ -z "${VERSION_URL}" ]; then
        echo "  ERRO: versão ${MC_VERSION} não encontrada"
        echo "  Versões disponíveis 1.21.x:"
        python3 - <<'PYEOF'
import urllib.request, json
with urllib.request.urlopen("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json") as r:
    manifest = json.load(r)
versions = [v["id"] for v in manifest["versions"] if v["id"].startswith("1.21")]
print("  " + ", ".join(versions[:15]))
PYEOF
        exit 1
    fi

    # Extrai URL do client jar
    CLIENT_URL=$(python3 - <<EOF
import urllib.request, json
with urllib.request.urlopen("${VERSION_URL}") as r:
    ver = json.load(r)
print(ver["downloads"]["client"]["url"])
EOF
)

    echo "  Baixando JAR..."
    curl -fsSL -o "${JAR_TARGET}" "${CLIENT_URL}"
    
    SIZE=$(du -sh "${JAR_TARGET}" | cut -f1)
    echo "  ✓ ${JAR_TARGET} (${SIZE})"
fi

# ── Sumário ───────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════"
echo " Setup completo!"
echo ""
echo " Próximo passo:"
echo "   python3 png_to_astc.py ${JAR_TARGET} --quality thorough"
echo ""
echo " Para teste rápido:"
echo "   python3 png_to_astc.py ${JAR_TARGET} --quality fast --dry-run"
echo "════════════════════════════════════════"
