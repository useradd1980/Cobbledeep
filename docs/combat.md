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

Minecraft weapon damage remains unchanged in this pass. Weapon damage dice,
critical hits, experience, additional hit dice, and a dedicated character HUD
can build on the same rule layer later.
