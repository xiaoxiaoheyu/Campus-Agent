#!/usr/bin/env python3
"""Pack clean project source into a zip (no node_modules / build / secrets / junk).

Usage (repo root):
  python scripts/pack_source.py
  python scripts/pack_source.py --output artifacts/submission/AgentA3-源码.zip
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import sys
import zipfile
from pathlib import Path, PurePosixPath

ROOT = Path(__file__).resolve().parents[1]
# Display / zip-root name (Unicode escapes keep the file encoding-safe on Windows).
PROJECT_NAME = "\u6821\u8fa9\u667a\u751f"  # 校辩智生
DEFAULT_OUTPUT = ROOT / "artifacts" / "submission" / "xiaobian-zhisheng-source.zip"

EXCLUDED_DIR_NAMES = {
    ".git",
    ".idea",
    ".vscode",
    ".cursor",
    ".worktrees",
    ".venv",
    "venv",
    ".uv-cache",
    ".pytest_cache",
    ".mypy_cache",
    ".ruff_cache",
    ".tox",
    ".cache",
    ".hbuilderx",
    ".claude",
    ".vs",
    ".agents",
    ".agnes",
    ".codegraph",
    ".codex",
    ".reasonix",
    ".superpowers",
    ".zcode",
    ".mimosa",
    "node_modules",
    "target",
    "dist",
    "unpackage",
    "build",
    "coverage",
    "__pycache__",
    ".data",
    ".redis-data",
    ".neo4j-data",
    "uploads",
    ".rag-data",
}

EXCLUDED_FILE_NAMES = {
    ".env",
    ".env.local",
    ".env.development.local",
    ".env.production.local",
    ".DS_Store",
    "Thumbs.db",
    "desktop.ini",
    "npm-debug.log",
    "yarn-error.log",
    # 内含演示服务器公网地址，不随提交包分发。
    ".env.production",
    # Root scratch files with no code references; not part of the submission.
    "reasonix.toml",
    "check_ai.sql",
    "campus-buildings.svg",
    "成长设定.md",
    "会议回放.md",
    "观测",
}

EXCLUDED_SUFFIXES = {
    ".pyc",
    ".pyo",
    ".class",
    ".jar",
    ".war",
    ".iml",
    ".log",
    ".tmp",
    # 生成文档（内嵌截图与旧数据），md 源已在包内。
    ".docx",
    ".pdf",
    ".temp",
    ".swp",
    ".swo",
    ".ibd",
    ".frm",
    ".myd",
    ".myi",
    ".rdb",
    ".aof",
}

# Top-level or nested path prefixes (posix) to skip entirely.
EXCLUDED_PREFIXES = (
    # iCAN 软件赛道不提交网页版学生端（App 对应的网页设计）。
    "Frontend/",
    # 设计稿与截图不是源码；方案 PDF 已含所需图。
    "designs/",
    "design-system/",
    "docs/design/",
    "docs/designs/",
    "docs/submission/figures/",
    "AppBackend/.data/",
    "AppBackend/.redis-data/",
    "AppBackend/uploads/",
    "AppBackend/logs/",
    "AppBackend/target/",
    "ai-servers/.rag-data/",
    "ai-servers/data/ai-exports/",
    "ai-servers/.tmp-ppt-",
    "artifacts/submission/",
    "docs/project-document/output/",
    "output/",
    "logs/",
    ".downloads/",
)


def _posix(path: Path) -> str:
    return path.as_posix()


def should_exclude(rel: Path) -> bool:
    logical = PurePosixPath(_posix(rel))
    parts = logical.parts
    if not parts or ".." in parts:
        return True
    if any(part in EXCLUDED_DIR_NAMES for part in parts):
        return True

    name = logical.name
    posix = _posix(rel)
    if name in EXCLUDED_FILE_NAMES:
        return True
    if name.startswith(".env.") and name.endswith(".local"):
        return True
    if posix in {"deploy/.env", "deploy/.env.server"}:
        return True
    if logical.suffix.lower() in EXCLUDED_SUFFIXES:
        return True
    if name.endswith(".inspect.ndjson"):
        return True
    if name.endswith((".keystore", ".jks")):
        return True
    if name.startswith("upload_") and len(parts) == 1 and logical.suffix.lower() in {
        ".jpg",
        ".jpeg",
        ".png",
        ".webp",
    }:
        return True

    for prefix in EXCLUDED_PREFIXES:
        if posix == prefix.rstrip("/") or posix.startswith(prefix):
            return True
    return False


def iter_source_files(root: Path) -> list[Path]:
    selected: list[Path] = []
    for path in root.rglob("*"):
        try:
            rel = path.relative_to(root)
        except ValueError:
            continue
        if any(part in EXCLUDED_DIR_NAMES for part in rel.parts):
            continue
        try:
            if not path.is_file() or path.is_symlink():
                continue
        except OSError:
            continue
        if should_exclude(rel):
            continue
        selected.append(rel)
    selected.sort(key=lambda p: _posix(p))
    return selected


def write_zip(root: Path, files: list[Path], output: Path) -> Path:
    output = output if output.is_absolute() else (root / output)
    output.parent.mkdir(parents=True, exist_ok=True)
    tmp = output.with_suffix(output.suffix + ".tmp")
    if tmp.exists():
        tmp.unlink()

    now = dt.datetime.now(dt.timezone.utc)
    stamp = (now.year, now.month, now.day, now.hour, now.minute, now.second)
    root_name = PROJECT_NAME

    with zipfile.ZipFile(tmp, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
        for rel in files:
            abs_path = root / rel
            info = zipfile.ZipInfo(f"{root_name}/{_posix(rel)}", stamp)
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            info.flag_bits |= 0x800  # UTF-8 path (校辩智生/...)
            zf.writestr(info, abs_path.read_bytes())

    if output.exists():
        output.unlink()
    tmp.replace(output)
    return output


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
        help=f"zip output path (default: {DEFAULT_OUTPUT.relative_to(ROOT).as_posix()})",
    )
    parser.add_argument("--list-only", action="store_true", help="only print file count / sample")
    args = parser.parse_args(argv)

    files = iter_source_files(ROOT)
    if not files:
        print("No source files found.", file=sys.stderr)
        return 1

    if args.list_only:
        print(f"files={len(files)}")
        for rel in files[:20]:
            print(_posix(rel))
        if len(files) > 20:
            print("...")
        return 0

    out = write_zip(ROOT, files, args.output)
    checksum = out.with_suffix(out.suffix + ".sha256")
    checksum.write_text(f"{sha256(out)}  {out.name}\n", encoding="utf-8")

    size_mb = out.stat().st_size / (1024 * 1024)
    print(f"OK  files={len(files)}  size={size_mb:.1f} MB")
    print(f"ZIP {out}")
    print(f"SUM {checksum}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
