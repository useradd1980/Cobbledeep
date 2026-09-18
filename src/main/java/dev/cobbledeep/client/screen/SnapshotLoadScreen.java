package dev.cobbledeep.client.screen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import dev.cobbledeep.client.save.GameSnapshotManager;
import dev.cobbledeep.client.save.GameSnapshotManager.Snapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class SnapshotLoadScreen extends Screen
{
    private static final int SAVES_PER_PAGE = 6;
    private final Screen parent;
    private final String status;
    private List<Snapshot> snapshots = List.of();
    private int page;

    public SnapshotLoadScreen(Screen parent)
    {
        this(parent, "");
    }

    private SnapshotLoadScreen(Screen parent, String status)
    {
        super(Component.literal("Load Cobbledeep Game"));
        this.parent = parent;
        this.status = status;
    }

    @Override
    protected void init()
    {
        snapshots = GameSnapshotManager.listSnapshots(minecraft.gameDirectory.toPath());
        int panelWidth = Math.min(430, width - 24);
        int left = (width - panelWidth) / 2;
        int top = Math.max(36, height / 2 - 94);
        int first = page * SAVES_PER_PAGE;
        int last = Math.min(snapshots.size(), first + SAVES_PER_PAGE);

        for (int index = first; index < last; index++)
        {
            Snapshot snapshot = snapshots.get(index);
            addRenderableWidget(Button.builder(Component.literal(snapshot.displayName()),
                    button -> confirmRestore(snapshot))
                    .bounds(left, top + (index - first) * 22, panelWidth, 20).build());
        }

        int bottom = top + SAVES_PER_PAGE * 22 + 8;
        Button previous = Button.builder(Component.literal("Previous"), button ->
        {
            page--;
            rebuildWidgets();
        }).bounds(left, bottom, 82, 20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);

        Button next = Button.builder(Component.literal("Next"), button ->
        {
            page++;
            rebuildWidgets();
        }).bounds(left + 88, bottom, 82, 20).build();
        next.active = (page + 1) * SAVES_PER_PAGE < snapshots.size();
        addRenderableWidget(next);

        addRenderableWidget(Button.builder(Component.literal("Other Worlds"),
                button -> minecraft.setScreen(new SelectWorldScreen(this)))
                .bounds(left + panelWidth - 170, bottom, 82, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_BACK,
                button -> minecraft.setScreen(parent))
                .bounds(left + panelWidth - 82, bottom, 82, 20).build());
    }

    private void confirmRestore(Snapshot snapshot)
    {
        minecraft.setScreen(new ConfirmScreen(confirmed ->
        {
            if (!confirmed)
            {
                minecraft.setScreen(this);
                return;
            }
            restore(snapshot);
        }, Component.literal("Restore this Cobbledeep save?"), Component.literal(
                "Progress made after this snapshot will be replaced."),
                Component.literal("Restore"), CommonComponents.GUI_CANCEL));
    }

    private void restore(Snapshot snapshot)
    {
        Minecraft client = minecraft;
        client.setScreen(new net.minecraft.client.gui.screens.GenericMessageScreen(
                Component.literal("Restoring Cobbledeep snapshot...")));
        CompletableFuture.runAsync(() ->
        {
            try
            {
                GameSnapshotManager.restoreSnapshot(client.gameDirectory.toPath(), snapshot);
            }
            catch (Exception exception)
            {
                throw new java.util.concurrent.CompletionException(exception);
            }
        }).whenComplete((ignored, failure) -> client.execute(() ->
        {
            if (failure != null)
            {
                String message = failure.getCause() == null ? failure.getMessage()
                        : failure.getCause().getMessage();
                client.setScreen(new SnapshotLoadScreen(parent,
                        "Restore failed: " + (message == null ? "Unknown error" : message)));
                return;
            }
            client.createWorldOpenFlows().openWorld(snapshot.worldId(),
                    () -> client.setScreen(new TitleScreen()));
        }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFD7C58A);
        if (snapshots.isEmpty())
            graphics.drawCenteredString(font, "No manual snapshots have been saved yet.",
                    width / 2, height / 2 - 8, 0xFF9EAAA1);
        if (!status.isEmpty())
            graphics.drawCenteredString(font, status, width / 2, height - 26, 0xFFFF7777);
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(parent);
    }
}
