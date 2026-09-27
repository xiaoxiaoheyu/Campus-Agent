#!/usr/bin/env python3
"""Stage the iCAN software-track submission into a standalone folder + zip."""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import shutil
import subprocess
import sys
import zipfile
from datetime import datetime
from pathlib import Path

REPO = Path(r"D:\software engineering\github\aic\AgentA3")
PACK_MODULE = REPO / "scripts" / "pack_source.py"

README_TEXT = """# 校辩智生 · iCAN 2026 AI应用创新挑战赛（软件赛道）源代码包

生成时间：{ts}
Git SHA：{sha}

本包只包含 iCAN 软件赛道提交物之三"可运行程序"的应用程序源代码。
应用方案 PDF 与演示视频 MP4 由团队另行提交，不在本包内。

## 目录

- `源码/`：可运行程序源代码。排除 Frontend 网页端、设计稿、截图、运行数据与密钥。
- 开源与 AI 工具标注随源码根目录分发：NOTICE、THIRD_PARTY_NOTICES.md、PRIVACY.md、SECURITY.md、AGENTS.md。

## 运行方式

见 `源码/README.md` 与 `源码/deploy/compose.submission.yml`；匿名测试账号 test_admin / admin123。
"""


def load_pack_module():
    spec = importlib.util.spec_from_file_location("pack_source", PACK_MODULE)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dest", required=True)
    args = parser.parse_args()

    dest = Path(args.dest) / "校辩智生"
    if dest.exists():
        shutil.rmtree(dest)
    code_dir = dest / "源码"

    pack = load_pack_module()
    files = pack.iter_source_files(REPO)
    for rel in files:
        target = code_dir / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(REPO / rel, target)

    sha = subprocess.run(
        ["git", "-C", str(REPO), "rev-parse", "HEAD"],
        capture_output=True, text=True, check=True,
    ).stdout.strip()
    (dest / "README.md").write_text(
        README_TEXT.format(ts=datetime.now().isoformat(timespec="seconds"), sha=sha),
        encoding="utf-8",
    )

    zip_path = Path(args.dest) / "校辩智生.zip"
    if zip_path.exists():
        zip_path.unlink()
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in sorted(dest.rglob("*")):
            if path.is_file():
                archive.write(path, path.relative_to(dest.parent))

    print(f"files={len(files)}")
    print(f"folder={dest}")
    print(f"zip={zip_path} sha256={sha256(zip_path)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
