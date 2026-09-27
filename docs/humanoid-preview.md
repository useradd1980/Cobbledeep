# Experimental animated humanoid and sword preview

The feature branch `feature/humanoid-glb-preview` contains the source code for the in-game tested skinned humanoid preview, rigid sword attachment, and corrected player orientation. This is a **local-player-only** render test. It retains the existing player renderer as a fallback and does not affect collision, hitboxes, networking, or combat.

## V2 GLB asset (local, not committed)

The September 27 V2 export corrects the grip and wrist poses while preserving the original 45-frame combat-idle Action, the 530-triangle skinned humanoid, all five sword meshes, and the animated `TwoHandedWeapon_ctrl`. Its exact SHA-256 is:

```
06fd4b842fd2ed001f95e227327c12917f5ac97ca8ef46280a93782a0f1219e2
```

Download `Cobbledeep_Humanoid_Animation_V2.glb` from the conversation and place it in your Downloads folder. From your Cobbledeep repository:

```bash
git pull --ff-only
bash tools/install-humanoid-v2.sh "$HOME/Downloads/Cobbledeep_Humanoid_Animation_V2.glb"
./gradlew runClient
```

The installer checks the exact SHA-256, backs up the previously installed GLB, and installs V2 at:

`src/main/resources/assets/cobbledeep/models/entity/humanoid_combat_idle.glb`

It is safe to re-run. The GLB and its backup are intentionally gitignored on this public feature branch. The installer itself is tracked in Git.

**Licensing:** The original humanoid and sword originated from third-party Sketchfab models. Their creator names, original URLs and license/attribution requirements have not yet been recorded. Verify these before adding the binary to public source control or distributing a release. The user-authored Blender animation remains in the locally installed test asset.

## Rendering details

The custom preview reads the first glTF animation, `TwoHanded_CombatIdle`, continuously. The exported sword meshes remain static on ordinary glTF reimport because Blender's Child Of constraint is not serialized. The Minecraft reader intentionally reconstructs the sword movement from the baked `TwoHandedWeapon_ctrl` animation and the opening pose, so the five rigid sword components follow the player's hands in-game.

The scene also contains an extra default Cube node. The loader explicitly locates the skinned humanoid primitive and five named sword meshes, so the Cube is not part of the in-game render.

The preview is enabled by the existing local `client` run configuration when the system property `cobbledeep.humanoidPreview` is `true`. The original ZIP installer already sets it locally. A missing or invalid GLB uses Cobbledeep's existing race-aware player renderer.

## Testing

Run `./gradlew runClient`, verify the weapon grip through frames 1–45, the seamless loop, and sword-follow movement. The V1 preview sword attachment and orientation have been confirmed in-game; V2 has passed GLB structural validation but still needs in-game visual confirmation. No change to the Blender IK rig is needed for this replacement.
