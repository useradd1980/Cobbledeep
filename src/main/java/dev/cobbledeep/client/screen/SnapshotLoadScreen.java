package dev.cobbledeep.client.screen;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import dev.cobbledeep.client.save.GameSnapshotManager;
import dev.cobbledeep.client.save.GameSnapshotManager.Snapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

public final class SnapshotLoadScreen extends Screen
{
    private static final int SAVES_PER_PAGE = 5;
    private final Screen parent;
    private final boolean allowSaving;
    private List<Snapshot> snapshots = List.of();
    private Snapshot selected;
    private EditBox nameField;
    private int page;
    private String status = "";
    private boolean busy;

    public SnapshotLoadScreen(Screen parent)
    {
        this(parent, false);
    }

    public SnapshotLoadScreen(Screen parent, boolean allowSaving)
    {
        super(Component.literal("Cobbledeep Save / Load"));
        this.parent = parent;
        this.allowSaving = allowSaving;
    }

    @Override
    protected void init()
    {
        snapshots = GameSnapshotManager.listSnapshots(minecraft.gameDirectory.toPath());
        int panelWidth = Math.min(500, width - 24);
        int left = (width - panelWidth) / 2;
        int top = Math.max(50, height / 2 - 92);

        if (allowSaving)
        {
            nameField = new EditBox(font, left, 30, panelWidth - 92, 20,
                    Component.literal("Save Name"));
            nameField.setMaxLength(40);
            nameField.setHint(Component.literal("Enter a save name"));
            if (selected != null) nameField.setValue(selected.name());
            addRenderableWidget(nameField);
            Button newSave = Button.builder(Component.literal("New Save"), button -> save(null))
                    .bounds(left + panelWidth - 86, 30, 86, 20).build();
            newSave.active = !busy;
            addRenderableWidget(newSave);
        }

        int first = page * SAVES_PER_PAGE;
        int last = Math.min(snapshots.size(), first + SAVES_PER_PAGE);
        for (int index = first; index < last; index++)
        {
            Snapshot snapshot = snapshots.get(index);
            String label = (snapshot.equals(selected) ? "> " : "") + snapshot.displayName();
            addRenderableWidget(Button.builder(Component.literal(label), button ->
            {
                selected = snapshot;
                status = "";
                rebuildWidgets();
            }).bounds(left, top + (index - first) * 22, panelWidth, 20).build());
        }

        int actionsY = top + SAVES_PER_PAGE * 22 + 6;
        Button overwrite = Button.builder(Component.literal("Overwrite"), button -> save(selected))
                .bounds(left, actionsY, 82, 20).build();
        overwrite.active = allowSaving && !busy && selected != null
                && selected.worldId().equals(currentWorldId());
        addRenderableWidget(overwrite);

        Button load = Button.builder(Component.literal("Load"), button -> confirmRestore(selected))
                .bounds(left + 88, actionsY, 82, 20).build();
        load.active = !busy && selected != null;
        addRenderableWidget(load);

        Button previous = Button.builder(Component.literal("Previous"), button ->
        {
            page--;
            rebuildWidgets();
        }).bounds(left, actionsY + 24, 82, 20).build();
        previous.active = page > 0;
        addRenderableWidget(previous);

        Button next = Button.builder(Component.literal("Next"), button ->
        {
            page++;
            rebuildWidgets();
        }).bounds(left + 88, actionsY + 24, 82, 20).build();
        next.active = (page + 1) * SAVES_PER_PAGE < snapshots.size();
        addRenderableWidget(next);

        if (!allowSaving)
            addRenderableWidget(Button.builder(Component.literal("Other Worlds"),
                    button -> minecraft.setScreen(new SelectWorldScreen(this)))
                    .bounds(left + panelWidth - 180, actionsY + 24, 92, 20).build());

        addRenderableWidget(Button.builder(allowSaving ? Component.literal("Return to Game")
                        : CommonComponents.GUI_BACK, button -> onClose())
                .bounds(left + panelWidth - 82, actionsY + 24, 82, 20).build());
    }

    private void save(Snapshot overwrite)
    {
        String name = nameField == null ? "" : nameField.getValue().strip();
        if (name.isEmpty())
        {
            status = "Enter a name for this save.";
            return;
        }
        busy = true;
        status = overwrite == null ? "Creating snapshot..." : "Overwriting snapshot...";
        rebuildWidgets();
        GameSnapshotManager.saveCurrentWorld(minecraft, name, overwrite, (success, message) ->
        {
            busy = false;
            status = message;
            if (success) selected = null;
            rebuildWidgets();
        });
    }

    private String currentWorldId()
    {
        var server = minecraft.getSingleplayerServer();
        return server == null ? "" : server.getWorldPath(LevelResource.ROOT)
                .toAbsolutePath().normalize().getFileName().toString();
    }

    private void confirmRestore(Snapshot snapshot)
    {
        if (snapshot == null) return;
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
        GenericMessageScreen progress = new GenericMessageScreen(
                Component.literal("Loading Cobbledeep snapshot..."));
        if (client.level != null)
        {
            dev.cobbledeep.Cobbledeep.LOGGER.info("Snapshot load: disconnecting active level");
            // Match Save and Quit: close the play connection BEFORE client teardown.
            client.level.disconnect();
            client.disconnect(progress);
            dev.cobbledeep.Cobbledeep.LOGGER.info("Snapshot load: client teardown returned");
        }
        client.setScreen(progress);
        restoreAfterShutdown(client, snapshot);
    }

    private static void restoreAfterShutdown(Minecraft client, Snapshot snapshot)
    {
        CompletableFuture.runAsync(() ->
        {
            try
            {
                dev.cobbledeep.Cobbledeep.LOGGER.info("Snapshot load: restoring archive {}", snapshot.archive());
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
                dev.cobbledeep.Cobbledeep.LOGGER.error("Snapshot load failed", failure);
                String message = failure.getCause() == null ? failure.getMessage()
                        : failure.getCause().getMessage();
                SnapshotLoadScreen screen = new SnapshotLoadScreen(new TitleScreen(), false);
                screen.status = "Restore failed: "
                        + (message == null ? "Unknown error" : message);
                client.setScreen(screen);
                return;
            }
            dev.cobbledeep.Cobbledeep.LOGGER.info("Snapshot load: opening restored world {}", snapshot.worldId());
            client.createWorldOpenFlows().openWorld(snapshot.worldId(),
                    () -> client.setScreen(new TitleScreen()));
        }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFD7C58A);
        if (snapshots.isEmpty())
            graphics.drawCenteredString(font, "No manual saves have been created yet.",
                    width / 2, height / 2 - 8, 0xFF9EAAA1);
        if (!status.isEmpty())
            graphics.drawCenteredString(font, status, width / 2, height - 18,
                    status.contains("failed") || status.startsWith("Enter")
                            ? 0xFFFF7777 : 0xFF9EAAA1);
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(allowSaving ? null : parent);
    }
}
