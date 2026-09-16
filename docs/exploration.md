# Terrain exploration and creature sight

Terrain uses a complete 24-block horizontal circle centred on the character.
Ground above and below the character is visible regardless of hills or walls.
The circle follows the interpolated character position every rendered frame;
newly visible terrain does not wait for an exploration packet. Camera movement
never moves the reveal circle or writes exploration.

Outside the circle, previously explored terrain is dimmed to 40% brightness;
unknown terrain is opaque. Normal Minecraft lighting still applies inside the
circle. Terrain discovery covers entire vertical columns, so surface and cave
terrain at the same X/Z share discovery. This is intentional for the full-height
radius rule. It does not remove roofs, ground or other geometry.

## Persistence and synchronization

The server discovers loaded columns on block movement and once per second while
stationary. It never loads missing chunks for discovery. Discovery also runs
outside tactical mode, but not for dead players or spectators. Columns that
intersect the radius are stored at two-block horizontal resolution, so memory
at the outer boundary can extend by up to one cell beyond the exact circle.
Current visibility itself is evaluated per pixel, not per cell.

Each player UUID has separate vanilla SavedData in each dimension:
`cobbledeep_exploration_<UUID>.dat`. Existing version-1 saves remain compatible;
new column discovery fills their existing 2x2x2-cell bitsets across world height.
Only newly discovered bits mark the file dirty. Normal world saving persists it,
and death does not erase it. No world reset is needed.

Changed sections sync in batches of at most 128 per tick. Joining, respawning or
changing dimension resets the client snapshot; disconnecting clears it. Protocol
2 still requires matching client/server versions. Local terrain is visible even
before the first snapshot; remembered terrain outside the circle waits for it.

## Creature sight

Creature models and nameplates retain their separate 360-degree, 24-block
three-dimensional line-of-sight checks from the character's eyes. Walls, closed
doors and terrain block these rays. Seven body samples allow partial exposure
through openings. Explored terrain never grants visibility of a creature.
The local character is never hidden. Glass currently blocks sight; water does
not. Enemy AI, sneaking cones and party sharing are unchanged.

## Rendering

Fog runs before the HUD and only in tactical mode; spectators are exempt. A
2 MiB atlas holds remembered cells in a 256-block cube around the camera.
Known terrain outside that window is concealed until the window reaches it;
the local visibility circle is evaluated before the atlas and remains clear.
Memory refreshes five times per second, with no terrain raycasting. Distant sky
is concealed. Translucency, particles and third-party shaders need in-game checks
because they can use different depth behaviour. Sounds and debug overlays are
unchanged. This is a visual gameplay feature, not an anti-cheat boundary.

## In-game checks

1. On an existing world, enable tactical mode and walk across stepped terrain.
   Nearby top faces, sides, plants and lower ground should all be free of fog.
2. Move uphill/downhill and rotate/zoom/pan: the 24-block horizontal circle
   must stay centred on the character, including on much higher/lower ground.
3. Move away: visited ground should dim, while distant unknown terrain remains
   opaque. Save, quit and reload; remembered terrain should remain remembered.
4. Put a creature behind a wall or closed door within the clear terrain circle:
   it must stay hidden until an opening gives actual character line of sight.
5. Test another player and dimension: records must remain separate. Check water,
   window resizing and Fast/Fancy/Fabulous graphics modes.

`/cobbledeep exploration status` reports stored cells and sections;
`/cobbledeep exploration check X Y Z` reports discovery without loading terrain.

## Offline checks

Compile `ExplorationGrid.java`, `FogVolume.java` and `TerrainRadius.java` with
`tests/ExplorationGridTest.java`, `tests/FogVolumeTest.java` and
`tests/TerrainRadiusTest.java`, then run those three test classes. They exercise
storage, atlas addressing, full-height disk coverage, negative coordinates,
world bounds, repeated discovery and save-payload restoration.

`python tests/fog_shader_test.py` uses headless Mesa EGL/OpenGL to compile the
actual GLSL resources and check depth copying, world reconstruction, clear
terrain at different elevations, opaque unknown terrain and dim memory after
the character moves away. It requires no Python packages. These checks do not
exercise the live Forge renderer or network lifecycle.
