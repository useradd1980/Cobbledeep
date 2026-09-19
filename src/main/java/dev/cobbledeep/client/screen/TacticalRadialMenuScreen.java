package dev.cobbledeep.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/** Client-side tactical action wheel. Actions are placeholders for now. */
public final class TacticalRadialMenuScreen extends Screen
{
    private static final String[] ACTIONS = {
            "Attack", "Spells", "Defend", "Use Item", "Inspect", "Talk"
    };
    private static final int INNER_RADIUS = 30;
    private static final int OUTER_RADIUS = 94;
    private static final int CELL_SIZE = 2;

    private final Component targetName;
    private int hovered = -1;

    public TacticalRadialMenuScreen(LivingEntity target)
    {
        super(Component.literal("Tactical Actions"));
        targetName = target.getDisplayName().copy();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        int centerX = width / 2;
        int centerY = height / 2;
        hovered = segmentAt(mouseX, mouseY, centerX, centerY);

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

        graphics.fill(centerX - 27, centerY - 14, centerX + 27, centerY + 14, 0xE0121814);
        graphics.drawCenteredString(font, targetName, centerX, centerY - 9, 0xE8D9A8);
        graphics.drawCenteredString(font, "Choose action", centerX, centerY + 2, 0xAAB5AA);

        for (int index = 0; index < ACTIONS.length; index++)
        {
            double angle = index * Math.PI * 2.0 / ACTIONS.length - Math.PI / 2.0;
            int labelX = centerX + (int)Math.round(Math.cos(angle) * 65.0);
            int labelY = centerY + (int)Math.round(Math.sin(angle) * 65.0) - 4;
            graphics.drawCenteredString(font, ACTIONS[index], labelX, labelY,
                    index == hovered ? 0xFFF1B8 : 0xE8E1C5);
        }

        graphics.drawCenteredString(font, "Right-click or Esc to close",
                centerX, centerY + OUTER_RADIUS + 10, 0xA0A0A0);
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
            int segment = segmentAt(mouseX, mouseY, width / 2, height / 2);
            if (segment >= 0)
            {
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

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
