# Tactical character relationships

Every living character can be classified as the player, a playable party NPC,
friendly, hostile or neutral. `CharacterRelations` is the single source of truth
for AI reactions and tactical-ring rendering.

The first test mappings are:

- Creeper: hostile, with a red ring.
- Skeleton: friendly, with a blue ring.
- A skeleton attacked by a player: permanently hostile, with a red ring.
- Playable NPC: reserved for future party members and uses the player's green ring.
- Neutral or unclassified creature: no tactical ring.

NPC rings use the same stationary single-band texture and dimensions as the
player ring. They render only while tactical mode is active and the character is
inside the player's current line of sight; remembered terrain never reveals an
NPC ring. Each ring follows its entity through the same frame-time smoothing used
by the player marker, including smooth vertical movement over uneven terrain.

Friendly mobs cannot select a player as an attack target. Attacking one changes
that individual entity to hostile before its retaliation target is assigned.
Explicit changes are stored in reserved scoreboard teams, so they persist with
the world and synchronize to clients. Scripts can change an NPC through
`CharacterRelations.setDisposition(entity, disposition)`.

Creative-mode target suppression remains Minecraft's responsibility: hostile
classification and red rings still apply, but creepers do not attack creative
players.
