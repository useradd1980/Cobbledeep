# Giant Rat — polygon model integration (work in progress)

This branch contains the first visual prototype of the Giant Rat: a manually summoned wandering entity, a custom polygon-mesh renderer, and idle/walk animation sampling from the authored Blockbench project. Combat, hit/death/detection state selection, natural spawning, blood effects and resource hot reload are not implemented yet.

The model is a Blockbench **Generic Model** with polygon meshes. Do not convert it to Minecraft cube geometry or GeckoLib's cube-only export. Keep the original animated `.bbmodel` file as the editable master.

## Required assets

The runtime expects these exact files:

- `src/main/resources/assets/cobbledeep/models/entity/giant_rat.json` — custom mesh, bone hierarchy and all six animation clips.
- `src/main/resources/assets/cobbledeep/textures/entity/giant_rat.png` — texture extracted from the same Blockbench project.

The two asset files are distributed as the Giant Rat assets-only ZIP in the conversation where this branch was created. They are not yet committed to GitHub; the GitHub connector available in that conversation supports UTF-8 file writes but not direct upload of the PNG bytes. The Java renderer will not work until **both** files are present.

To regenerate the two assets after editing the source project, run from your Cobbledeep checkout:

```bash
python3 tools/export_giant_rat.py /path/to/cobbledeep_giant_rat_animated.bbmodel
```

Then stage and commit them to this feature branch:

```bash
git add src/main/resources/assets/cobbledeep/models/entity/giant_rat.json src/main/resources/assets/cobbledeep/textures/entity/giant_rat.png
git commit -m "Add authored Giant Rat model, texture and six animations"
git push origin feature/giant-rat
```

## First test

Launch using `./gradlew runClient`, open a creative test world with cheats enabled, and summon with `/summon cobbledeep:giant_rat ~ ~ ~`. Inspect the model's geometry, scale and heading and check idle/walk switching. Compile and in-game rendering have not been verified; report the first Gradle error or a screenshot of rendering problems so we can correct them before wiring up combat animations.
