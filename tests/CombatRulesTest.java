import dev.cobbledeep.character.CharacterClass;
import dev.cobbledeep.combat.CombatRules;

public final class CombatRulesTest
{
    private static int checks;

    public static void main(String[] args)
    {
        check(CombatRules.hitDie(CharacterClass.FIGHTER) == 10, "fighters use a d10");
        check(CombatRules.hitDie(CharacterClass.CLERIC) == 8, "clerics use a d8");
        check(CombatRules.hitDie(CharacterClass.THIEF) == 6, "thieves use a d6");
        check(CombatRules.hitDie(CharacterClass.MAGE) == 4, "mages use a d4");

        check(CombatRules.levelOneHitPoints(CharacterClass.FIGHTER, 18) == 14,
                "warriors receive the full Constitution bonus");
        check(CombatRules.levelOneHitPoints(CharacterClass.MAGE, 18) == 6,
                "non-warrior Constitution bonuses are capped");
        check(CombatRules.levelOneHitPoints(CharacterClass.MAGE, 3) == 2,
                "low Constitution reduces maximum hit points");

        check(CombatRules.thac0(CharacterClass.FIGHTER, 1) == 20, "level-one warrior THAC0");
        check(CombatRules.thac0(CharacterClass.FIGHTER, 5) == 16, "warrior THAC0 progression");
        check(CombatRules.thac0(CharacterClass.CLERIC, 4) == 18, "priest THAC0 progression");
        check(CombatRules.thac0(CharacterClass.THIEF, 5) == 18, "rogue THAC0 progression");
        check(CombatRules.thac0(CharacterClass.MAGE, 4) == 19, "mage THAC0 progression");

        check(CombatRules.strengthAttackAdjustment(7, 0) == -1, "low Strength penalty");
        check(CombatRules.strengthAttackAdjustment(17, 0) == 1, "high Strength bonus");
        check(CombatRules.strengthAttackAdjustment(18, 75) == 2, "exceptional Strength bonus");
        check(CombatRules.dexterityArmorClassAdjustment(18) == -4, "high Dexterity improves AC");
        check(CombatRules.dexterityArmorClassAdjustment(5) == 2, "low Dexterity worsens AC");

        check(CombatRules.requiredRoll(20, 10, 0) == 10, "unarmored target threshold");
        check(CombatRules.requiredRoll(20, 4, 2) == 14, "armor and Strength affect threshold");
        check(!CombatRules.hits(1, 20, 10, 20), "natural one always misses");
        check(CombatRules.hits(20, 20, -10, -20), "natural twenty always hits");
        check(!CombatRules.hits(13, 20, 4, 2), "roll below threshold misses");
        check(CombatRules.hits(14, 20, 4, 2), "roll at threshold hits");

        System.out.println("Passed " + checks + " combat-rule checks.");
    }

    private static void check(boolean condition, String message)
    {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
