# Experimental animated humanoid and sword preview

The feature branch `feature/humanoid-glb-preview` contains the source code for the in-game tested skinned humanoid preview, rigid sword attachment, and corrected player orientation. This is a **local-player-only** render test. It retains the existing player renderer as a fallback and does not affect collision, hitboxes, networking, or combat.

## V4 GLB asset (local, not committed)

The September 27 V4 export incorporates the missing grip.L Location keyframe and adjusts the evaluated left-hand IK/arm pose while preserving the original 45-frame combat-idle Action, the 530-triangle skinned humanoid, all five sword meshes, and the animated `TwoHandedWeapon_ctrl`. Its exact SHA-256 is:

```
636d0f0f31c84c1aa716df9215eec3569dec0ad7bdb9ad6dd56f21ae25e72f42
```

Download `Cobbledeep_Humanoid_Animation_V4.glb` from the conversation and place it in your Downloads folder. From your Cobbledeep repository:

```bash
git pull --ff-only
bash tools/install-humanoid-v4.sh "$HOME/Downloads/Cobbledeep_Humanoid_Animation_V4.glb"
./gradlew runClient
```

The installer checks the exact SHA-256, backs up the previously installed GLB, and installs V4 at:

`src/main/resources/assets/cobbledeep/models/entity/humanoid_combat_idle.glb`

It is safe to re-run. The GLB and its backup are intentionally gitignored on this public feature branch. The installer itself is tracked in Git.

**Licensing:** The original humanoid and sword originated from third-party Sketchfab models. Their creator names, original URLs and license/attribution requirements have not yet been recorded. Verify these before adding the binary to public source control or distributing a release. The user-authored Blender animation remains in the locally installed test asset.

## Rendering details

The custom preview reads the first glTF animation, `TwoHanded_CombatIdle`, continuously. The exported sword meshes remain static on ordinary glTF reimport because Blender's Child Of constraint is not serialized. The Minecraft reader intentionally reconstructs the sword movement from the baked `TwoHandedWeapon_ctrl` animation and the opening pose, so the five rigid sword components follow the player's hands in-game.

The scene also contains an extra default Cube node. The loader explicitly locates the skinned humanoid primitive and five named sword meshes, so the Cube is not part of the in-game render.

The preview is enabled by the existing local `client` run configuration when the system property `cobbledeep.humanoidPreview` is `true`. The original ZIP installer already sets it locally. A missing or invalid GLB uses Cobbledeep's existing race-aware player renderer.

## Sword size and grip calibration

The preview now scales the exported sword by **1.30× by default**, about the bounding-box centre of the actual `Handle_lambert1_0` mesh. It does **not** resize the humanoid or shift the sword around `sword1`'s distant object origin. The five sword parts still follow the baked `TwoHandedWeapon_ctrl`.

A log line printed when the asset loads reports the loaded GLB's SHA-256, scale, handle pivot and offset. Verify the SHA matches the V4 value listed above so we're not calibrating against an older asset.

For one-off tuning, set the environment variables on the **same shell command** as the Gradle run. These are optional, and their defaults are 1.30 and zero offsets:

```bash
# Compare the original GLB scale at exactly frame 1:
COBBLEDEEP_HUMANOID_FREEZE_FRAME1=true COBBLEDEEP_HUMANOID_SWORD_SCALE=1.0 ./gradlew runClient

# Test the current default sword size without an explicit scale override:
COBBLEDEEP_HUMANOID_FREEZE_FRAME1=true ./gradlew runClient
```

For fine positioning, `COBBLEDEEP_HUMANOID_SWORD_OFFSET_X`, `..._Y` and `..._Z` apply extra translation in exported glTF **world units**, before the weapon-control movement (1 source unit = 1/70 Minecraft block in this preview). Example: `COBBLEDEEP_HUMANOID_SWORD_OFFSET_X=1`. All are zero by default. These are development-time visual calibration controls, not new character/equipment systems.

If the frozen V4 pose still differs from Blender with scale set to 1.0 and offsets zero, investigate the underlying Java mesh/skin-space math rather than changing the working master rig or hiding the discrepancy with large offsets.

## Testing

Run `./gradlew runClient`, verify the weapon grip through frames 1–45, the seamless loop, and sword-follow movement. The V1 preview sword attachment and orientation have been confirmed in-game; V4 has passed GLB structural, finite-value, seamless-loop and handle-proximity checks but still needs in-game visual confirmation. No change to the Blender IK rig is needed for this replacement.

## Reproduce the Blender opening-frame grip in Minecraft

For an exact opening-pose comparison, from the repository root run:

```bash
COBBLEDEEP_HUMANOID_FREEZE_FRAME1=true ./gradlew runClient
```

This freezes **only the preview GLB animation sampling** at time 0 (Blender frame 1). The actual Minecraft player can still move and rotate. The preview uses the same sampled bone matrices for both the skinned hands and the sword's rigid attachment. The usual `./gradlew runClient` leaves the animation looping normally. A log message confirms when freeze mode is active.

Compare with `Cobbledeep_Humanoid_Animation_V4.glb` imported into Blender at frame 1, hiding the stray Cube. Use roughly the same camera angle. If the sword/hand alignment differs even when frozen, investigate the Java mesh/skin/object coordinate transforms before compensating with manual sword offsets or changes to the Blender rig.
