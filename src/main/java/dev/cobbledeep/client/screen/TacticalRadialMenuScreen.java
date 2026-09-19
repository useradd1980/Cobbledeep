package dev.cobbledeep.client.screen;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Client-side tactical action wheel. Actions are placeholders for now. */
public final class TacticalRadialMenuScreen extends Screen
{
    private static final String[] ACTIONS = {
            "Attack", "Spells", "Defend", "Use Item", "Inspect", "Talk"
    };
    private static final int INNER_RADIUS = 11;
    private static final int OUTER_RADIUS = 34;
    private static final int CELL_SIZE = 1;
    private static final float LABEL_SCALE = 0.55F;
    private static final float TARGET_SCALE = 0.5F;
    private static final float TACTICAL_FOV = 50.0F;
    private static final int QUICK_WEAPON_COUNT = 4;
    private static final int WEAPON_SLOT_SIZE = 18;
    private static int lastWeaponSlot;

    private final Component targetName;
    private final double anchorX;
    private final double anchorY;
    private int hovered = -1;
    private int hoveredWeapon = -1;
    private boolean attackSubmenuOpen;

    public TacticalRadialMenuScreen(LivingEntity target)
    {
        super(Component.literal("Tactical Actions"));
        targetName = target.getDisplayName().copy();
        double[] anchor = projectTarget(target);
        anchorX = anchor[0];
        anchorY = anchor[1];
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        int centerX = (int)Math.round(anchorX * width);
        int centerY = (int)Math.round(anchorY * height);
        hovered = segmentAt(mouseX, mouseY, centerX, centerY);
        if (hovered == 0) attackSubmenuOpen = true;
        hoveredWeapon = attackSubmenuOpen
                ? weaponSlotAt(mouseX, mouseY, centerX, centerY) : -1;

        int innerSquared = INNER_RADIUS * INNER_RADIUS;
        int outerSquared = OUTER_RADIUS * OUTER_RADIUS;
        for (int y = -OUTER_RADIUS; y <= OUTER_RADIUS; y += CELL_SIZE)
        {
            for (int x = -OUTER_RADIUS; x <= OUTER_RADIUS; x += CELL_SIZE)
            {
                int distanceSquared = x * x + y * y;
                if (distanceSquared < innerSquared || distanceSquared > outerSquared) continue;
                int segment = segmentAt(centerX + x, centerY + y, centerX, centerY);
                int colour = segment == hovered ? 0xE0A07732
                        : (segment & 1) == 0 ? 0xD02C352E : 0xD0343E36;
                graphics.fill(centerX + x, centerY + y,
                        centerX + x + CELL_SIZE, centerY + y + CELL_SIZE, colour);
            }
        }

        graphics.fill(centerX - INNER_RADIUS, centerY - INNER_RADIUS,
                centerX + INNER_RADIUS, centerY + INNER_RADIUS, 0xE0121814);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX, centerY - 2, 0.0F);
        graphics.pose().scale(TARGET_SCALE, TARGET_SCALE, 1.0F);
        graphics.drawCenteredString(font, targetName, 0, 0, 0xE8D9A8);
        graphics.pose().popPose();

        for (int index = 0; index < ACTIONS.length; index++)
        {
            double angle = index * Math.PI * 2.0 / ACTIONS.length - Math.PI / 2.0;
            int labelX = centerX + (int)Math.round(Math.cos(angle) * 23.0);
            int labelY = centerY + (int)Math.round(Math.sin(angle) * 23.0);
            graphics.pose().pushPose();
            graphics.pose().translate(labelX, labelY - 2, 0.0F);
            graphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1.0F);
            graphics.drawCenteredString(font, ACTIONS[index], 0, 0,
                    index == hovered ? 0xFFF1B8 : 0xE8E1C5);
            graphics.pose().popPose();
        }

        if (attackSubmenuOpen)
            renderWeaponSubmenu(graphics, mouseX, mouseY, centerX, centerY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 1)
        {
            onClose();
            return true;
        }
        if (button == 0)
        {
            int centerX = (int)Math.round(anchorX * width);
            int centerY = (int)Math.round(anchorY * height);
            int weaponSlot = attackSubmenuOpen
                    ? weaponSlotAt(mouseX, mouseY, centerX, centerY) : -1;
            if (weaponSlot >= 0 && minecraft.player != null)
            {
                ItemStack weapon = minecraft.player.getInventory().getItem(weaponSlot);
                if (!weapon.isEmpty())
                {
                    lastWeaponSlot = weaponSlot;
                    showAttackPlaceholder(weapon);
                    return true;
                }
            }

            int segment = segmentAt(mouseX, mouseY, centerX, centerY);
            if (segment >= 0)
            {
                if (segment == 0)
                {
                    showAttackPlaceholder(selectedWeapon());
                    return true;
                }
                Component action = Component.literal(ACTIONS[segment] + " — ")
                        .append(targetName).append(" (coming soon)");
                onClose();
                if (minecraft.player != null)
                    minecraft.player.displayClientMessage(action, true);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderWeaponSubmenu(GuiGraphics graphics, int mouseX, int mouseY,
                                     int centerX, int centerY)
    {
        int left = centerX - QUICK_WEAPON_COUNT * WEAPON_SLOT_SIZE / 2;
        int top = centerY - OUTER_RADIUS - WEAPON_SLOT_SIZE - 3;
        for (int slot = 0; slot < QUICK_WEAPON_COUNT; slot++)
        {
            int x = left + slot * WEAPON_SLOT_SIZE;
            int background = slot == hoveredWeapon ? 0xE0A07732
                    : slot == lastWeaponSlot ? 0xD06D793E : 0xD0202822;
            graphics.fill(x, top, x + 17, top + 17, background);
            ItemStack weapon = minecraft.player == null
                    ? ItemStack.EMPTY : minecraft.player.getInventory().getItem(slot);
            if (!weapon.isEmpty())
            {
                graphics.renderItem(weapon, x, top);
                graphics.renderItemDecorations(font, weapon, x, top);
            }
        }

        if (hoveredWeapon >= 0 && minecraft.player != null)
        {
            ItemStack weapon = minecraft.player.getInventory().getItem(hoveredWeapon);
            Component label = weapon.isEmpty() ? Component.literal("Empty") : weapon.getHoverName();
            graphics.pose().pushPose();
            graphics.pose().translate(centerX, top - 7, 0.0F);
            graphics.pose().scale(LABEL_SCALE, LABEL_SCALE, 1.0F);
            graphics.drawCenteredString(font, label, 0, 0, 0xFFF1B8);
            graphics.pose().popPose();
        }
    }

    private ItemStack selectedWeapon()
    {
        if (minecraft.player == null) return ItemStack.EMPTY;
        ItemStack selected = minecraft.player.getInventory().getItem(lastWeaponSlot);
        if (!selected.isEmpty()) return selected;
        for (int slot = 0; slot < QUICK_WEAPON_COUNT; slot++)
        {
            ItemStack candidate = minecraft.player.getInventory().getItem(slot);
            if (!candidate.isEmpty())
            {
                lastWeaponSlot = slot;
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }

    private void showAttackPlaceholder(ItemStack weapon)
    {
        Component weaponName = weapon.isEmpty()
                ? Component.literal("Unarmed") : weapon.getHoverName();
        Component action = Component.literal("Attack with ").append(weaponName)
                .append(" — ").append(targetName).append(" (coming soon)");
        onClose();
        if (minecraft.player != null)
            minecraft.player.displayClientMessage(action, true);
    }

    private static int weaponSlotAt(double mouseX, double mouseY, int centerX, int centerY)
    {
        int left = centerX - QUICK_WEAPON_COUNT * WEAPON_SLOT_SIZE / 2;
        int top = centerY - OUTER_RADIUS - WEAPON_SLOT_SIZE - 3;
        if (mouseX < left || mouseX >= left + QUICK_WEAPON_COUNT * WEAPON_SLOT_SIZE
                || mouseY < top || mouseY >= top + 17)
            return -1;
        return (int)(mouseX - left) / WEAPON_SLOT_SIZE;
    }

    private static int segmentAt(double mouseX, double mouseY, int centerX, int centerY)
    {
        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distanceSquared = dx * dx + dy * dy;
        if (distanceSquared < INNER_RADIUS * INNER_RADIUS
                || distanceSquared > OUTER_RADIUS * OUTER_RADIUS)
            return -1;

        double clockwiseFromTop = Math.atan2(dy, dx) + Math.PI / 2.0;
        if (clockwiseFromTop < 0.0) clockwiseFromTop += Math.PI * 2.0;
        return (int)Math.floor((clockwiseFromTop + Math.PI / ACTIONS.length)
                / (Math.PI * 2.0 / ACTIONS.length)) % ACTIONS.length;
    }

    private static double[] projectTarget(LivingEntity target)
    {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        double screenWidth = minecraft.getWindow().getScreenWidth();
        double screenHeight = minecraft.getWindow().getScreenHeight();
        if (!camera.isInitialized() || screenWidth <= 0.0 || screenHeight <= 0.0)
            return cursorAnchor(minecraft, screenWidth, screenHeight);

        Vec3 targetPoint = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        Vec3 relative = targetPoint.subtract(camera.getPosition());
        Vec3 forward = new Vec3(camera.getLookVector());
        Vec3 left = new Vec3(camera.getLeftVector());
        Vec3 up = new Vec3(camera.getUpVector());
        double depth = relative.dot(forward);
        if (depth <= 0.001)
            return cursorAnchor(minecraft, screenWidth, screenHeight);

        double tanHalfFov = Math.tan(Math.toRadians(TACTICAL_FOV * 0.5));
        double aspect = screenWidth / screenHeight;
        double ndcX = -relative.dot(left) / (depth * tanHalfFov * aspect);
        double ndcY = relative.dot(up) / (depth * tanHalfFov);
        return new double[] {(ndcX + 1.0) * 0.5, (1.0 - ndcY) * 0.5};
    }

    private static double[] cursorAnchor(Minecraft minecraft, double width, double height)
    {
        return new double[] {
                width <= 0.0 ? 0.5 : minecraft.mouseHandler.xpos() / width,
                height <= 0.0 ? 0.5 : minecraft.mouseHandler.ypos() / height
        };
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
