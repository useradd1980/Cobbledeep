package dev.cobbledeep.client.screen;

import java.io.InputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.mojang.blaze3d.platform.NativeImage;
import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.client.save.GameSnapshotManager;
import dev.cobbledeep.client.save.GameSnapshotManager.Snapshot;
import dev.cobbledeep.client.save.SnapshotThumbnailCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.storage.LevelResource;

public final class SnapshotLoadScreen extends Screen
{
    private static final int ROW_HEIGHT = 62;
    private static final int THUMBNAIL_WIDTH = 96;
    private static final int THUMBNAIL_HEIGHT = 54;
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final Screen parent;
    private final boolean allowSaving;
    private record Thumbnail(ResourceLocation texture, int width, int height) { }

    private final Map<Snapshot, Thumbnail> thumbnails = new HashMap<>();
    private List<Snapshot> snapshots = List.of();
    private Snapshot selected;
    private EditBox nameField;
    private Button overwriteButton;
    private Button loadButton;
    private Button deleteButton;
    private int listLeft;
    private int listRight;
    private int listTop;
    private int listBottom;
    private double scroll;
    private String status = "";

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
        releaseThumbnails();
        snapshots = GameSnapshotManager.listSnapshots(minecraft.gameDirectory.toPath());
        loadThumbnails();

        int panelWidth = Math.min(560, width - 24);
        listLeft = (width - panelWidth) / 2;
        listRight = listLeft + panelWidth;
        listTop = allowSaving ? 58 : 34;
        listBottom = Math.max(listTop + ROW_HEIGHT, height - 54);
        clampScroll();

        if (allowSaving)
        {
            nameField = new EditBox(font, listLeft, 30, panelWidth - 92, 20,
                    Component.literal("Save Name"));
            nameField.setMaxLength(40);
            nameField.setHint(Component.literal("Enter a save name"));
            if (selected != null) nameField.setValue(selected.name());
            addRenderableWidget(nameField);
            addRenderableWidget(Button.builder(Component.literal("New Save"), button -> save(null))
                    .bounds(listRight - 86, 30, 86, 20).build());
        }

        int actionsY = height - 44;
        overwriteButton = addRenderableWidget(Button.builder(Component.literal("Overwrite"),
                button -> save(selected)).bounds(listLeft, actionsY, 82, 20).build());
        loadButton = addRenderableWidget(Button.builder(Component.literal("Load"),
                button -> confirmRestore(selected)).bounds(listLeft + 88, actionsY, 72, 20).build());
        deleteButton = addRenderableWidget(Button.builder(Component.literal("Delete"),
                button -> confirmDelete(selected)).bounds(listLeft + 166, actionsY, 72, 20).build());

        if (!allowSaving)
            addRenderableWidget(Button.builder(Component.literal("Other Worlds"),
                    button -> minecraft.setScreen(new SelectWorldScreen(this)))
                    .bounds(listRight - 180, actionsY, 92, 20).build());

        addRenderableWidget(Button.builder(allowSaving ? Component.literal("Return to Game")
                        : CommonComponents.GUI_BACK, button -> onClose())
                .bounds(listRight - 82, actionsY, 82, 20).build());
        updateActions();
    }

    private void save(Snapshot overwrite)
    {
        String name = nameField == null ? "" : nameField.getValue().strip();
        if (name.isEmpty())
        {
            status = "Enter a name for this save.";
            return;
        }
        if (!SnapshotThumbnailCapture.request(minecraft, name, overwrite))
            status = "Unable to capture this snapshot right now.";
    }

    private void select(Snapshot snapshot)
    {
        selected = snapshot;
        status = "";
        if (nameField != null) nameField.setValue(snapshot.name());
        updateActions();
    }

    private void updateActions()
    {
        if (overwriteButton != null)
            overwriteButton.active = allowSaving && selected != null
                    && selected.worldId().equals(currentWorldId());
        if (loadButton != null) loadButton.active = selected != null;
        if (deleteButton != null) deleteButton.active = selected != null;
    }

    private String currentWorldId()
    {
        var server = minecraft.getSingleplayerServer();
        return server == null ? "" : server.getWorldPath(LevelResource.ROOT)
                .toAbsolutePath().normalize().getFileName().toString();
    }

    private void confirmDelete(Snapshot snapshot)
    {
        if (snapshot == null) return;
        minecraft.setScreen(new ConfirmScreen(confirmed ->
        {
            if (confirmed)
            {
                try
                {
                    GameSnapshotManager.deleteSnapshot(snapshot);
                    selected = null;
                    status = "Snapshot deleted.";
                }
                catch (Exception exception)
                {
                    Cobbledeep.LOGGER.error("Unable to delete Cobbledeep snapshot", exception);
                    status = "Delete failed: " + (exception.getMessage() == null
                            ? exception.getClass().getSimpleName() : exception.getMessage());
                }
            }
            minecraft.setScreen(this);
        }, Component.literal("Delete this Cobbledeep save?"),
                Component.literal("This snapshot cannot be recovered."),
                Component.literal("Delete"), CommonComponents.GUI_CANCEL));
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
            Cobbledeep.LOGGER.info("Snapshot load: disconnecting active level");
            client.level.disconnect();
            client.disconnect(progress);
            Cobbledeep.LOGGER.info("Snapshot load: client teardown returned");
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
                Cobbledeep.LOGGER.info("Snapshot load: restoring archive {}", snapshot.archive());
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
                Cobbledeep.LOGGER.error("Snapshot load failed", failure);
                String message = failure.getCause() == null ? failure.getMessage()
                        : failure.getCause().getMessage();
                SnapshotLoadScreen screen = new SnapshotLoadScreen(new TitleScreen(), false);
                screen.status = "Restore failed: "
                        + (message == null ? "Unknown error" : message);
                client.setScreen(screen);
                return;
            }
            Cobbledeep.LOGGER.info("Snapshot load: opening restored world {}", snapshot.worldId());
            client.createWorldOpenFlows().openWorld(snapshot.worldId(),
                    () -> client.setScreen(new TitleScreen()));
        }));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        // Screen background blur is finalized while vanilla widgets render.
        // Draw our custom rows afterwards so their text and previews stay sharp.
        renderSnapshotList(graphics, mouseX, mouseY);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFD7C58A);
        if (snapshots.isEmpty())
            graphics.drawCenteredString(font, "No manual saves have been created yet.",
                    width / 2, (listTop + listBottom) / 2 - 4, 0xFF9EAAA1);
        if (!status.isEmpty())
            graphics.drawCenteredString(font, status, width / 2, height - 18,
                    status.contains("failed") || status.startsWith("Enter")
                            ? 0xFFFF7777 : 0xFF9EAAA1);
    }

    private void renderSnapshotList(GuiGraphics graphics, int mouseX, int mouseY)
    {
        graphics.fill(listLeft, listTop, listRight, listBottom, 0xB0101713);
        graphics.enableScissor(listLeft, listTop, listRight, listBottom);
        for (int index = 0; index < snapshots.size(); index++)
        {
            Snapshot snapshot = snapshots.get(index);
            int y = listTop + index * ROW_HEIGHT - (int) scroll;
            if (y + ROW_HEIGHT <= listTop || y >= listBottom) continue;
            boolean hovered = mouseX >= listLeft && mouseX < listRight - 8
                    && mouseY >= y && mouseY < y + ROW_HEIGHT;
            int background = snapshot.equals(selected) ? 0xC0445D50
                    : hovered ? 0xA02D3D35 : (index % 2 == 0 ? 0x60232E28 : 0x401B241F);
            graphics.fill(listLeft + 2, y + 1, listRight - 9, y + ROW_HEIGHT - 1, background);

            int thumbnailX = listLeft + 6;
            int thumbnailY = y + 4;
            Thumbnail thumbnail = thumbnails.get(snapshot);
            if (thumbnail != null)
                graphics.blit(thumbnail.texture(), thumbnailX, thumbnailY,
                        THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, 0, 0,
                        thumbnail.width(), thumbnail.height(),
                        thumbnail.width(), thumbnail.height());
            else
            {
                graphics.fill(thumbnailX, thumbnailY, thumbnailX + THUMBNAIL_WIDTH,
                        thumbnailY + THUMBNAIL_HEIGHT, 0xFF0A0F0C);
                graphics.drawCenteredString(font, "No Preview", thumbnailX + THUMBNAIL_WIDTH / 2,
                        thumbnailY + 23, 0xFF6F7B73);
            }

            int textX = thumbnailX + THUMBNAIL_WIDTH + 10;
            graphics.drawString(font, snapshot.name(), textX, y + 7, 0xFFD7C58A, false);
            graphics.drawString(font, DISPLAY_TIME.format(Instant.ofEpochMilli(snapshot.savedAt())),
                    textX, y + 21, 0xFFB7C1BA, false);
            graphics.drawString(font, snapshot.characterName() + "  •  " + snapshot.worldName(),
                    textX, y + 35, 0xFF9EAAA1, false);
            graphics.drawString(font, snapshot.x() + ", " + snapshot.y() + ", " + snapshot.z(),
                    textX, y + 47, 0xFF758078, false);
        }
        graphics.disableScissor();
        renderScrollbar(graphics);
    }

    private void renderScrollbar(GuiGraphics graphics)
    {
        int contentHeight = snapshots.size() * ROW_HEIGHT;
        int viewHeight = listBottom - listTop;
        if (contentHeight <= viewHeight) return;
        int trackLeft = listRight - 7;
        int thumbHeight = Math.max(24, viewHeight * viewHeight / contentHeight);
        int thumbY = listTop + (int) (scroll * (viewHeight - thumbHeight) / maxScroll());
        graphics.fill(trackLeft, listTop, listRight - 2, listBottom, 0x80303934);
        graphics.fill(trackLeft, thumbY, listRight - 2, thumbY + thumbHeight, 0xFF839187);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && mouseX >= listLeft && mouseX < listRight
                && mouseY >= listTop && mouseY < listBottom)
        {
            int index = (int) ((mouseY - listTop + scroll) / ROW_HEIGHT);
            if (index >= 0 && index < snapshots.size()) select(snapshots.get(index));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY)
    {
        if (mouseX >= listLeft && mouseX < listRight && mouseY >= listTop && mouseY < listBottom)
        {
            scroll = Mth.clamp(scroll - scrollY * ROW_HEIGHT / 2.0, 0.0, maxScroll());
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int maxScroll()
    {
        return Math.max(0, snapshots.size() * ROW_HEIGHT - (listBottom - listTop));
    }

    private void clampScroll()
    {
        scroll = Mth.clamp(scroll, 0.0, maxScroll());
    }

    private void loadThumbnails()
    {
        for (Snapshot snapshot : snapshots)
        {
            if (!Files.isRegularFile(snapshot.thumbnail())) continue;
            try (InputStream input = Files.newInputStream(snapshot.thumbnail());
                    NativeImage image = NativeImage.read(input))
            {
                DynamicTexture texture = new DynamicTexture(image.getWidth(), image.getHeight(), false);
                NativeImage pixels = texture.getPixels();
                if (pixels == null)
                {
                    texture.close();
                    continue;
                }
                pixels.copyFrom(image);
                texture.setFilter(true, false);
                texture.upload();
                ResourceLocation location = minecraft.getTextureManager().register(
                        "cobbledeep_snapshot_" + snapshot.savedAt(), texture);
                thumbnails.put(snapshot, new Thumbnail(location, image.getWidth(), image.getHeight()));
            }
            catch (Exception exception)
            {
                Cobbledeep.LOGGER.warn("Unable to load snapshot thumbnail {}",
                        snapshot.thumbnail(), exception);
            }
        }
    }

    private void releaseThumbnails()
    {
        if (minecraft != null)
            thumbnails.values().forEach(thumbnail ->
                    minecraft.getTextureManager().release(thumbnail.texture()));
        thumbnails.clear();
    }

    @Override
    public void removed()
    {
        releaseThumbnails();
    }

    @Override
    public void onClose()
    {
        minecraft.setScreen(allowSaving ? null : parent);
    }
}
