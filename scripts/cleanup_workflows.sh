#!/usr/bin/env bash
set -euo pipefail

# 使用方法：
#   bash scripts/cleanup_workflows.sh [repo_root=. ] [--apply]
# 示例：
#   bash scripts/cleanup_workflows.sh .          # 只演练，列出将删除的文件
#   bash scripts/cleanup_workflows.sh . --apply  # 真正删除并推送一个 PR 分支

WORKDIR="${1:-.}"
DRY_RUN=1
if [[ "${2:-}" == "--apply" ]]; then DRY_RUN=0; fi

# 需要保留的工作流文件名（按需修改）
KEEP=("build_apk_v035.yml")

WF_DIR="$WORKDIR/.github/workflows"
if [[ ! -d "$WF_DIR" ]]; then
  echo "No $WF_DIR directory. Nothing to do."
  exit 0
fi

shopt -s nullglob
to_delete=()
for f in "$WF_DIR"/*.yml "$WF_DIR"/*.yaml; do
  base="$(basename "$f")"
  skip=0
  for k in "${KEEP[@]}"; do
    [[ "$base" == "$k" ]] && { skip=1; break; }
  done
  [[ $skip -eq 1 ]] && continue
  to_delete+=("$f")
done
shopt -u nullglob

if [[ ${#to_delete[@]} -eq 0 ]]; then
  echo "No old workflows to delete. All good."
  exit 0
fi

echo "Will delete ${#to_delete[@]} workflow file(s):"
printf '  - %s\n' "${to_delete[@]}"

if [[ $DRY_RUN -eq 1 ]]; then
  echo "Dry run only. Pass --apply to actually delete."
  exit 0
fi

# 真删 + 提交 + 推送 PR 分支
git rev-parse --is-inside-work-tree >/dev/null 2>&1 || { echo "Not inside a git repo."; exit 1; }
branch="chore/cleanup-workflows-$(date +%Y%m%d-%H%M%S)"
git checkout -b "$branch"
git rm -f "${to_delete[@]}"
git commit -m "chore: remove old GitHub Actions workflows"
git push -u origin "$branch"
echo "Pushed branch: $branch"
echo "Open a PR to merge the cleanup."
