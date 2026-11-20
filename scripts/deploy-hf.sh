#!/usr/bin/env bash
# Deploy the committed code on the current branch to a Hugging Face Docker Space.
#
# Usage (Git Bash):  scripts/deploy-hf.sh
# One-time setup:    git remote add hf https://huggingface.co/spaces/<user>/<space>
#
# Hugging Face rejects ordinary git pushes that contain binary files, and the Space only needs the code,
# so this pushes a snapshot WITHOUT report/ (UML PNGs and the report draft). The README gets the Space
# configuration header. Each run adds one commit on top of the Space's previous snapshot; the GitHub
# history and your working tree are not touched. Uncommitted changes are NOT deployed.
set -euo pipefail

REMOTE="${HF_REMOTE:-hf}"
cd "$(git rev-parse --show-toplevel)"
git remote get-url "$REMOTE" >/dev/null

tmp_index="$(mktemp)"
trap 'rm -f "$tmp_index"' EXIT
export GIT_INDEX_FILE="$tmp_index"

git read-tree HEAD
git rm -r -q --cached --ignore-unmatch report

header=$'---\ntitle: GradPath AI\nemoji: 🎓\ncolorFrom: indigo\ncolorTo: blue\nsdk: docker\napp_port: 8080\npinned: false\nshort_description: Career readiness and placement platform (Java Spring Boot)\n---\n\n'
readme_blob="$( { printf '%s' "$header"; git show HEAD:README.md; } | git hash-object -w --stdin )"
git update-index --add --cacheinfo 100644,"$readme_blob",README.md

tree="$(git write-tree)"
parent_args=()
if git fetch -q "$REMOTE" main 2>/dev/null; then
  parent_args=(-p FETCH_HEAD)
fi
commit="$(git commit-tree "$tree" "${parent_args[@]}" -m "Deploy $(git rev-parse --short HEAD)")"

git push "$REMOTE" "$commit:refs/heads/main"
echo "Pushed $(git rev-parse --short HEAD) to $REMOTE. The Space now rebuilds (see its Logs tab)."
