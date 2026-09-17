# Cobbledeep combat rules

The first combat pass replaces the player's survival-health foundation with an
AD&D-style rules layer while leaving Minecraft's damage amounts in place.

- New characters begin at level 1 and receive their maximum class hit die plus
  their Constitution adjustment. Current health continues to use Minecraft's
  normal save data.
- Hunger, saturation, and exhaustion are neutralized. Natural regeneration is
  disabled, while deliberate healing such as spells and potions still works.
- Player melee attacks roll a d20 against `THAC0 - target AC - Strength bonus`.
  A natural 1 always misses and a natural 20 always hits.
- Armour converts from Minecraft armour points to descending AC: no armour is
  AC 10, full leather is AC 7, chain is AC 5, iron is AC 4, and diamond or
  netherite is AC 2. A held shield improves AC by 1. Created player characters
  also receive their Dexterity AC adjustment.
- The action bar shows every melee roll, its target number, THAC0, AC, and the
  final hit or miss result.
- Equipped Minecraft swords, axes, tridents, maces, bows, and crossbows map to
  Cobbledeep weapon proficiency categories and AD&D-style damage dice. Weapon
  proficiency penalties or specialization bonuses affect melee attack rolls;
  specialization and Strength affect melee damage. The character record shows
  both base and effective THAC0 so equipping a different weapon is immediately
  visible without changing the character's underlying class progression.

Minecraft weapon damage remains unchanged in this pass. Weapon damage dice,
critical hits, experience, additional hit dice, and a dedicated character HUD
can build on the same rule layer later.

Press **C** in game to open the character record. It displays identity, level,
ability scores, current and maximum hit points, hit die, THAC0, live armour
class, and melee attack adjustment. The level-up control is intentionally
disabled until experience and advancement choices are implemented.

The character record has a new **Inventory** page. Cobbledeep uses one full-suit
Armour slot and has no leggings slot. If leggings
are equipped through another interface, they are moved safely back to the
backpack (or dropped when the backpack is full). The page reserves AD&D-style
positions for an amulet, cloak, two rings, gauntlets, belt, ammunition, weapon
sets, and quick items. The Minecraft inventory key opens this server-backed
screen. Backpack and hotbar positions reference the normal player inventory,
while accessory positions are separately persisted with the player. Items can
be dragged, split, swapped, or shift-clicked using normal container behaviour.
Until Cobbledeep accessory items are introduced, each accessory position accepts
one item so persistence and inventory handling can be tested.

The compact Inventory page uses a paper-doll layout with a live 3D player model
surrounded by equipment and accessory positions. Its live combat summary sits
beside the model, while the backpack and hotbar occupy the lower half. Armour
Class updates as armour is moved; the weapon, proficiency, attack adjustment,
effective THAC0, and damage update from the currently selected hotbar weapon
without requiring a return to the character record.
