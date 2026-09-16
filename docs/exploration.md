# Exploration and character sight — first pass

Discovery is calculated by the server from the player's eyes, in all directions,
within 24 blocks. The tactical camera's position never contributes to discovery.
Rays stop at actual block collision shapes, including walls and closed doors.
Water does not block sight. Glass currently blocks it, just like other full
collision blocks; optical material rules can be added later.

Discovery uses 2x2x2-block cells, stored as 512 bits per populated 16x16x16 section.
A partly visible cell is marked explored as a whole. This coarse memory is never
used to decide whether a creature is currently visible. Separate vertical cells
prevent surface exploration from revealing caves directly below.

## Saving

Each player UUID has its own `cobbledeep_exploration_<UUID>.dat` in each dimension's
vanilla `data` folder. The world owns the records, so death/respawn does not clear
them. Vanilla autosave and clean world shutdown write dirty records. The format
is versioned; unchanged observations do not mark records dirty. Dirty records
are rewritten as a whole in this first pass, not as individual section files.
Back up the entire world to preserve exploration along with everything else.

Discovery runs regardless of tactical mode, but does not run for dead players or
spectators. A scan is limited to 96 rays per player per server tick and cycles
through nearby 3D cell centres in roughly four seconds at normal tick rate.
Newly exposed rooms therefore fill in over several ticks. A ray marks only its
unobstructed segment and the surface it hits. Missing chunks are not loaded by
the scanner. No client packet can mark a location explored.

## Current creature visibility

In tactical mode, living-entity models and nameplates are hidden unless at least
one of seven body samples is visible from the player's interpolated eyes within
24 blocks. This includes other players but never hides the local character.
Changing camera angle, panning or prior discovery does not bypass walls. Leaving
tactical mode restores normal Minecraft rendering. Spectators are exempt.

## Visual terrain fog

In tactical mode, a depth-aware render pass now conceals unknown world surfaces
with opaque dark fog, dims remembered surfaces to 40% brightness when out of
sight, and leaves currently visible surfaces at normal brightness. It runs
before the HUD, so menus and controls remain legible. The distant sky is also
concealed to avoid revealing unexplored terrain silhouettes against it.

The client receives only its own server discovery record, in batches of at most
128 sections. Joining, respawning or changing dimension starts a fresh snapshot;
later packets update changed sections. Disconnecting clears the client cache and
GPU textures. Existing version-1 exploration save files are unchanged. Network
protocol 2 requires matching updated mod versions on server and client.

Current terrain visibility is recalculated from the character's eyes five times
per second, independently of permanent discovery. The rendered fog samples the
same 2-block 3D cells as storage: partially seen cells can expose a small area at
a doorway, but creature visibility still uses its separate multi-point checks.
Discovery can take roughly four seconds to fill in after entering a new area.
Camera panning and rotation never add discovery. The first snapshot must arrive
before any terrain is revealed.

The GPU uses a 256-block-wide cube that follows the camera; known surfaces outside
that window also remain concealed until panning brings them into it. Its byte
atlas is 2 MiB. Current sight checks are restricted to the 24-block player radius.
This is an initial performance budget to test on the ThinkPad, not a benchmark.

The pass covers final world pixels using scene depth. Translucent surfaces,
depth-independent nameplates/particles, and third-party shader pipelines can have
different depth behaviour and need in-game testing. Sounds and debug overlays
are not hidden. This is not an anti-cheat boundary; Minecraft still sends tracked
entities to the client. Enemy AI, roof cutaways and party sharing are unchanged.

## In-game checks

1. Run `/cobbledeep exploration status`, wait five seconds, then run it again.
   This reports only your record in the current dimension; no cheats are needed.
2. Use `/cobbledeep exploration check X Y Z` on a known room/cave cell. Walk to a
   position with a clear view, wait for a scan, and check again. Coordinate checks
   do not themselves reveal or load the target.
3. Walk far enough away that the test cell is outside the 24-block sight range.
   Save and quit, rejoin, and check those same coordinates before returning. They
   should remain explored. A cell behind an unopened wall must remain unknown.
4. In tactical mode, put a mob behind a solid wall taller than both creatures.
   Panning the camera around the wall should not expose its model/nameplate.
   Approach the doorway: partial body visibility should reveal it. Retreat and
   it should disappear again. Try opening/closing the door while stationary.
5. Check the same X/Z at cave height and at the surface, and test another player,
   dimension and world. Records must remain independent. Death must retain them.
6. Enable tactical mode with V and wait for initial discovery. Pan well beyond the
   character: unknown terrain should be opaque. Walk into an area, then move away:
   it should remain dimly visible. Turning the camera alone must not clear fog.
7. Rotate/zoom over a slope and resize the window: fog should stay attached to
   world surfaces. Save and rejoin away from the explored area, then pan back to
   it: remembered terrain should be dim. Check water and Fast/Fancy/Fabulous modes.

For a deterministic room test, use a closed roof as well as walls. Open tops and
windows genuinely allow eye-level rays through. Keep test points at least two
blocks beyond a wall to avoid mistaking a partly visible cell for a whole room.

## Offline storage checks

From the repository root, with a JDK installed:

```sh
mkdir -p build/exploration-tests
javac -d build/exploration-tests src/main/java/dev/cobbledeep/exploration/ExplorationGrid.java tests/ExplorationGridTest.java
java -cp build/exploration-tests ExplorationGridTest
```

These compile and test the actual storage index, signed coordinates, unique cell
indices, independent records and section-payload round trips. They do not replace
a Forge build or the in-game NBT save/load and doorway checks above.

The fog atlas can also be tested without Minecraft by compiling `FogVolume.java`
and `tests/FogVolumeTest.java` alongside the grid and running `FogVolumeTest`.
`python tests/fog_shader_test.py` uses headless Mesa EGL/OpenGL to compile the actual
GLSL resources and verify opaque/dim/clear pixels, depth copying, out-of-window
masking and rotated/translated world-position reconstruction. It requires Mesa
EGL/OpenGL libraries but no Python packages. These tests do not exercise Forge's
live rendering hooks or network lifecycle.
