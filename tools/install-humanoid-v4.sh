#!/usr/bin/env bash
# Installs the validated V4 combat-idle GLB into an existing Cobbledeep checkout.
# It intentionally does not commit third-party Blender assets to the public repo.
set -euo pipefail
source_glb="${1:-$HOME/Downloads/Cobbledeep_Humanoid_Animation_V4.glb}"
expected='636d0f0f31c84c1aa716df9215eec3569dec0ad7bdb9ad6dd56f21ae25e72f42'
if [[ ! -f "$source_glb" ]]; then
    printf 'GLB not found: %s\nDownload the V4 GLB from the conversation first.\n' "$source_glb" >&2
    exit 1
fi
actual="$(sha256sum "$source_glb")"
actual="${actual%% *}"
if [[ "$actual" != "$expected" ]]; then
    printf 'Incorrect GLB: SHA-256 %s\nExpected: %s\n' "$actual" "$expected" >&2
    exit 1
fi
root="$(git rev-parse --show-toplevel)"
dest="$root/src/main/resources/assets/cobbledeep/models/entity/humanoid_combat_idle.glb"
mkdir -p "$(dirname "$dest")"
if [[ -f "$dest" ]]; then
    present="$(sha256sum "$dest")"
    present="${present%% *}"
    if [[ "$present" == "$expected" ]]; then
        printf 'V4 humanoid animation already installed: %s\n' "$dest"
        exit 0
    fi
    backup="$dest.backup-$(date +%Y%m%d-%H%M%S)"
    cp -p -- "$dest" "$backup"
    printf 'Previous GLB backed up to %s\n' "$backup"
fi
install -m 0644 -- "$source_glb" "$dest"
printf 'Installed V4 GLB: %s\nSHA-256: %s\n' "$dest" "$expected"
