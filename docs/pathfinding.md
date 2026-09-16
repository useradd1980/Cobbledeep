# Tactical click-to-move

Left-click a walkable surface to plan a route. The action bar briefly shows
"Finding route..."; the character then follows the route with normal movement.
Right-click without dragging cancels. Dragging right-click still rotates the
camera. Pressing a movement/jump/sneak key cancels the route and returns control
to the keyboard. Opening a menu, losing window focus, dying, changing world or
leaving tactical mode also stops movement and releases automated inputs.

Routes use A* over block-centred positions, with feet height stored in sixteenths
of a block. Actual collision shapes and a standing player-sized body determine
clearance. Open doors, ordinary stairs/slabs, one-block jumps and drops of at
most one block are supported. Level and uphill diagonals require both adjoining
cardinal cells to be safely reachable, with heights between the source and
destination. The full diagonal still needs swept-body collision clearance,
preventing corner cutting. Downhill changes continue to use cardinal steps.
Diagonal jumps start upon entering the validated edge, accounting for the longer
distance between diagonal block centres. Jumping requires
additional headroom. Creative flying, swimming, ladders, parkour, crouch-only
passages, automatic door opening and moving platforms are outside this pass.

Closed doors remain obstacles. Water/lava, fire, cactus, magma, campfires,
powder snow, berry bushes and wither roses are excluded. Modded hazards are not
automatically classified. The pathfinder reads only loaded chunks inside the
world border and never loads new ones. Clicking a surface chooses the nearest
standing position within its neighbouring columns and about one block vertically;
it can reject a wall, inaccessible roof or other non-walkable target.

Searches process at most 48 queue entries per client tick, with a 4,096-expansion
limit and a 64-block bound on each axis from the start. Difficult searches can
take several seconds and may ask for a closer target. Failure stops movement;
it never falls back to walking straight through an obstacle. Waypoints are
rechecked while walking. A stall is measured from actual three-dimensional
player displacement, not distance to an exact waypoint centre; moving around a
corner or over uneven terrain therefore does not cause a false replan. Routine
replanning runs incrementally while the player
continues along the still-safe active route, then joins the furthest directly
reachable point on the replacement route without turning back toward its search
origin. A genuinely unsafe next edge still stops movement. Brief failed checks
stop input and are rechecked for
three ticks before replanning. Repeated failures in one place allow at most two
replans; reaching a later waypoint at least 0.75 blocks away restores that
allowance. A retry also measures its current position against the original
failure location, so route handoffs cannot prevent genuine travel from restoring
the allowance and separate recoveries do not exhaust a whole-trip budget.
No movement teleports, speed changes or new server packets are used.
Exploration, lighting and creature line of sight are unchanged.

Horizontal collision checks use a continuous swept player box against the
actual collision shapes, rather than rejecting everything inside the enclosing
rectangle of a diagonal move. Walls and thin door shapes still block movement;
unused corners of that rectangle no longer cause false obstructions. Hazard
and loaded-chunk checks remain conservative over the enclosing bounds. Starting
or replanning chooses the nearest reachable standing node, skipping closer
nodes that cannot be entered from the player's actual position.

Waypoint arrival uses progress along the incoming segment as well as distance,
so passing a node during a jump/drop does not trigger a turn back to its centre.
On straight stair runs the follower can advance to the next node while airborne
only if the normal collision checks approve that edge from its actual position.
Otherwise forward input is released while the player finishes landing, without
reversing their heading. Corners are not skipped during that height wait. A
route's initial graph node is skipped only when the next edge is already safe,
avoiding unnecessary recentering after a replan.

Height-wait lookahead requires a straight segment only while airborne. After
landing, a turn may advance when the normal collision checks approve travel
from the player's actual body to the next node. This prevents a safe uphill
corner from waiting thirty ticks and launching a `landing height wait` replan.

When the horizontal plane is reached before an ascent is complete, the follower
preserves the validated edge's heading and continues forward/jump input until
the player's feet reach the higher surface. This applies to final uphill steps
and uphill corners as well as straight stair runs. Descents retain the separate
landing wait above.

For the first six grounded ticks after completing a climb, a transient
collision from the live post-jump body is checked again from the next edge's
canonical graph origin.
This keeps movement held while the body settles against the ledge. The planned
edge is still collision-checked, so a newly placed block or closed door stops it.
Straight uphill lookahead can complete a waypoint while the player is above its
landing plane; that handoff now starts the same grounded transition. Replan
messages include their trigger while this movement behaviour is being verified.

## Verification

Compile `src/main/java/dev/cobbledeep/pathfinding/GridPathfinder.java` and
`tests/GridPathfinderTest.java` and run `GridPathfinderTest`. Tests cover wall
detours through a doorway, closed/changed openings, elevation, separate floors,
unreachable destinations and bounded incremental work. Random maps compare
reachability and shortest paths with an independent breadth-first search.
These tests exercise the search engine, not Minecraft collision or input APIs.
Compile `WaypointProgress.java` with `tests/WaypointProgressTest.java` and run
`WaypointProgressTest` for jump/drop overshoot, cardinal/diagonal movement,
negative coordinates, corners, switchbacks and final-destination tolerance.
Compile `WalkStepRules.java`, `GridPathfinder.java` and `tests/WalkStepRulesTest.java`
and run `WalkStepRulesTest` for supported diagonal climbs, side gaps/walls,
jump timing and an A* route up a synthetic terraced hill.
Compile `SweptBody.java`, `RouteRecovery.java` and `tests/RouteRecoveryTest.java`
and run `RouteRecoveryTest` for off-route obstructions, thin doors, wall/ground
contact, randomized collision samples and recovery limits with/without progress.
Compile `MovementProgress.java` with `tests/MovementProgressTest.java` and run
`MovementProgressTest` for stationary detection, small positional jitter and
continuous horizontal/vertical movement.

In Minecraft, check:

1. Click across flat ground, then around a wall with an open doorway.
2. Climb a slab/stair staircase and a one-block ledge with clear headroom.
3. Try a closed room, two-block cliff, water and lava. No unsafe direct fallback.
4. Close a door/block a corridor during movement: reroute or stop.
5. Cancel while searching, walking and jumping; test right-click, movement keys,
   menu opening, loss of focus, leaving tactical mode and changing worlds.
6. Test narrow doorways and corners at different approach angles. Collision and
   momentum behaviour require an in-game run on the installed Forge version.
7. Walk up and down consecutive one-block ledges, including a corner immediately
   after a ledge. Check for backtracking, missed landings and cancellation mid-jump.
8. Climb consecutive ledges diagonally in all four directions. Check narrow
   uphill corners and low ceilings too: these must still block unsafe diagonals.
