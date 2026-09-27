# Experimental animated humanoid and sword preview

The feature branch `feature/humanoid-glb-preview` contains the source code for the working skinned humanoid preview and the first rigid sword attachment. This is a **local-player-only** render test. It retains the existing player renderer as a fallback and does not affect collision, hitboxes, networking, or combat.

## Local GLB asset (not committed)

Place the previously validated 45-frame export at:

`src/main/resources/assets/cobbledeep/models/entity/humanoid_combat_idle.glb`

The same original exported GLB works for the sword: its five sword meshes and animated `TwoHandedWeapon_ctrl` remain in the file even though the sword is outside the visible glTF scene. Do not replace it with a freshly exported humanoid-only asset. The validated preview asset has SHA-256:

`602494ead7dff89132ce0a33e4ea80a445908eb5e3d87f427f4e094eb9b5f1bf`

The third-party Sketchfab character and sword GLB are **not included in this public branch** pending verification and documentation of both original creators, URLs, license terms, and required attribution. Before publishing game releases or adding the asset to this public repository, verify those terms.

## Enable the local preview

The preview defaults to off. For the local Forge `client` run, add this to the `minecraft { runs { ... } }` block in `build.gradle` if not already present:

```groovy
register('client') {
    systemProperty 'cobbledeep.humanoidPreview', 'true'
}
```

The original ZIP installer already made this local change. Disable by setting the property to `false`. A missing or invalid GLB reverts to Cobbledeep's existing race-aware player renderer.

## What it draws

- Skinned humanoid (four influences per vertex) playing the first baked glTF animation, `TwoHanded_CombatIdle`, in a loop.
- Rigid sword parts: blade, guard, handle, handhold, and pommel. The weapon follows the evaluated movement of `TwoHandedWeapon_ctrl` relative to the opening animation frame.
- Neutral placeholder colors; not yet connected to inventory, equipment visibility, character appearance, or multiple animation states.
- Blender IK and Child Of constraints do not run in Minecraft. Export bakes the evaluated deform-bone and weapon-control animation.

## Test

Run `./gradlew runClient`. Verify facing direction, idle loop, weapon placement, and that the sword does not separate during the movement. Turn the preview off to confirm fallback rendering.

The original blender rig remains the editable master. This branch contains the tested source-code patch, but the sword attachment has not yet been verified in-game.
