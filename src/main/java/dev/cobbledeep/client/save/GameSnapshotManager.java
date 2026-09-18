package dev.cobbledeep.client.save;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import dev.cobbledeep.Cobbledeep;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelResource;

public final class GameSnapshotManager
{
    private static final int MAX_SNAPSHOTS = 10;
    private static final String SNAPSHOT_DIRECTORY = "cobbledeep-snapshots";
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss-SSS").withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());
    private static final AtomicBoolean SAVING = new AtomicBoolean();

    public record Snapshot(Path archive, String worldId, String worldName, String name,
            String characterName, long savedAt, int x, int y, int z, boolean autosave)
    {
        public String displayName()
        {
            return name + "  —  " + DISPLAY_TIME.format(Instant.ofEpochMilli(savedAt)) + "  "
                    + characterName + "  (" + x + ", " + y + ", " + z + ")";
        }
    }

    private GameSnapshotManager() {}

    public static void saveCurrentWorld(Minecraft minecraft, String saveName,
            Snapshot overwrite, BiConsumer<Boolean, String> completion)
    {
        if (minecraft.player == null || minecraft.getSingleplayerServer() == null)
        {
            showMessage(minecraft, "Snapshots are available in singleplayer games.");
            completion.accept(false, "Snapshots are available in singleplayer games.");
            return;
        }
        if (!SAVING.compareAndSet(false, true))
        {
            showMessage(minecraft, "A snapshot is already being saved.");
            completion.accept(false, "A snapshot is already being saved.");
            return;
        }

        var server = minecraft.getSingleplayerServer();
        Path gameDirectory = minecraft.gameDirectory.toPath().toAbsolutePath().normalize();
        String characterName = minecraft.player.getName().getString();
        int x = minecraft.player.blockPosition().getX();
        int y = minecraft.player.blockPosition().getY();
        int z = minecraft.player.blockPosition().getZ();
        showMessage(minecraft, "Saving Cobbledeep snapshot...");

        server.execute(() ->
        {
            try
            {
                server.saveEverything(false, true, false);
                Path worldDirectory = server.getWorldPath(LevelResource.ROOT)
                        .toAbsolutePath().normalize();
                createSnapshot(gameDirectory, worldDirectory, server.getWorldData().getLevelName(),
                        saveName, characterName, x, y, z);
                if (overwrite != null && overwrite.worldId().equals(worldDirectory.getFileName().toString()))
                    deleteSnapshot(overwrite);
                prune(snapshotRoot(gameDirectory).resolve(worldDirectory.getFileName().toString()));
                minecraft.execute(() ->
                {
                    showMessage(minecraft, "Cobbledeep snapshot saved.");
                    completion.accept(true, "Snapshot saved.");
                });
            }
            catch (Exception exception)
            {
                Cobbledeep.LOGGER.error("Unable to save Cobbledeep snapshot", exception);
                minecraft.execute(() ->
                {
                    String message = "Snapshot failed: " + safeMessage(exception);
                    showMessage(minecraft, message);
                    completion.accept(false, message);
                });
            }
            finally
            {
                SAVING.set(false);
            }
        });
    }

    public static List<Snapshot> listSnapshots(Path gameDirectory)
    {
        Path root = snapshotRoot(gameDirectory);
        if (!Files.isDirectory(root)) return List.of();

        List<Snapshot> snapshots = new ArrayList<>();
        try (var paths = Files.walk(root, 2))
        {
            paths.filter(path -> path.getFileName().toString().endsWith(".properties"))
                    .forEach(path -> readSnapshot(path).ifPresent(snapshots::add));
        }
        catch (IOException exception)
        {
            Cobbledeep.LOGGER.error("Unable to list Cobbledeep snapshots", exception);
        }
        snapshots.sort(Comparator.comparingLong(Snapshot::savedAt).reversed());
        return List.copyOf(snapshots);
    }

    public static void restoreSnapshot(Path gameDirectory, Snapshot snapshot) throws IOException
    {
        Path saves = gameDirectory.toAbsolutePath().normalize().resolve("saves");
        Path activeWorld = saves.resolve(snapshot.worldId()).normalize();
        if (!activeWorld.getParent().equals(saves))
            throw new IOException("Invalid snapshot world directory");

        Path rollback = saves.resolve(snapshot.worldId() + ".cobbledeep-rollback-"
                + System.currentTimeMillis());
        boolean movedActiveWorld = false;
        try
        {
            if (Files.exists(activeWorld))
            {
                Files.move(activeWorld, rollback);
                movedActiveWorld = true;
            }
            Files.createDirectories(activeWorld);
            unzip(snapshot.archive(), activeWorld);
            if (movedActiveWorld) deleteRecursively(rollback);
        }
        catch (Exception exception)
        {
            Path failedRestore = saves.resolve(snapshot.worldId() + ".cobbledeep-failed-"
                    + System.currentTimeMillis());
            if (Files.exists(activeWorld)) Files.move(activeWorld, failedRestore);
            if (movedActiveWorld && Files.exists(rollback))
                Files.move(rollback, activeWorld, StandardCopyOption.REPLACE_EXISTING);
            deleteRecursively(failedRestore);
            if (exception instanceof IOException ioException) throw ioException;
            throw new IOException("Unable to restore snapshot", exception);
        }
    }

    private static void createSnapshot(Path gameDirectory, Path worldDirectory, String worldName,
            String saveName, String characterName, int x, int y, int z) throws IOException
    {
        String worldId = worldDirectory.getFileName().toString();
        long savedAt = System.currentTimeMillis();
        Path directory = Files.createDirectories(snapshotRoot(gameDirectory).resolve(worldId));
        String baseName = "save-" + FILE_TIME.format(Instant.ofEpochMilli(savedAt));
        Path temporaryArchive = directory.resolve(baseName + ".zip.tmp");
        Path archive = directory.resolve(baseName + ".zip");

        try
        {
            zipWorld(worldDirectory, temporaryArchive);
            try
            {
                Files.move(temporaryArchive, archive, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (AtomicMoveNotSupportedException exception)
            {
                Files.move(temporaryArchive, archive);
            }

            Properties metadata = new Properties();
            metadata.setProperty("version", "1");
            metadata.setProperty("worldId", worldId);
            metadata.setProperty("worldName", worldName);
            metadata.setProperty("name", saveName.strip());
            metadata.setProperty("characterName", characterName);
            metadata.setProperty("savedAt", Long.toString(savedAt));
            metadata.setProperty("x", Integer.toString(x));
            metadata.setProperty("y", Integer.toString(y));
            metadata.setProperty("z", Integer.toString(z));
            metadata.setProperty("autosave", "false");
            try (OutputStream output = Files.newOutputStream(directory.resolve(baseName + ".properties")))
            {
                metadata.store(output, "Cobbledeep snapshot");
            }
        }
        finally
        {
            Files.deleteIfExists(temporaryArchive);
        }
    }

    private static void zipWorld(Path worldDirectory, Path destination) throws IOException
    {
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(
                Files.newOutputStream(destination))); var paths = Files.walk(worldDirectory))
        {
            for (Path path : paths.filter(Files::isRegularFile).toList())
            {
                Path relative = worldDirectory.relativize(path);
                if (relative.toString().equals("session.lock")) continue;
                ZipEntry entry = new ZipEntry(relative.toString().replace('\\', '/'));
                entry.setTime(Files.getLastModifiedTime(path).toMillis());
                zip.putNextEntry(entry);
                Files.copy(path, zip);
                zip.closeEntry();
            }
        }
    }

    private static void unzip(Path archive, Path destination) throws IOException
    {
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(
                Files.newInputStream(archive))))
        {
            for (ZipEntry entry; (entry = zip.getNextEntry()) != null; )
            {
                Path output = destination.resolve(entry.getName()).normalize();
                if (!output.startsWith(destination)) throw new IOException("Invalid snapshot entry");
                if (entry.isDirectory()) Files.createDirectories(output);
                else
                {
                    Files.createDirectories(output.getParent());
                    try (OutputStream file = new BufferedOutputStream(Files.newOutputStream(output)))
                    {
                        zip.transferTo(file);
                    }
                }
                zip.closeEntry();
            }
        }
    }

    private static java.util.Optional<Snapshot> readSnapshot(Path metadataPath)
    {
        Properties metadata = new Properties();
        try (InputStream input = Files.newInputStream(metadataPath))
        {
            metadata.load(input);
            if (!"1".equals(metadata.getProperty("version"))) return java.util.Optional.empty();
            String filename = metadataPath.getFileName().toString();
            Path archive = metadataPath.resolveSibling(
                    filename.substring(0, filename.length() - ".properties".length()) + ".zip");
            if (!Files.isRegularFile(archive)) return java.util.Optional.empty();
            long savedAt = Long.parseLong(metadata.getProperty("savedAt"));
            String defaultName = "Save " + DISPLAY_TIME.format(Instant.ofEpochMilli(savedAt));
            return java.util.Optional.of(new Snapshot(archive,
                    metadata.getProperty("worldId"), metadata.getProperty("worldName", "Cobbledeep"),
                    metadata.getProperty("name", defaultName),
                    metadata.getProperty("characterName", "Character"),
                    savedAt,
                    Integer.parseInt(metadata.getProperty("x", "0")),
                    Integer.parseInt(metadata.getProperty("y", "0")),
                    Integer.parseInt(metadata.getProperty("z", "0")),
                    Boolean.parseBoolean(metadata.getProperty("autosave", "false"))));
        }
        catch (Exception exception)
        {
            Cobbledeep.LOGGER.warn("Ignoring invalid snapshot metadata {}", metadataPath, exception);
            return java.util.Optional.empty();
        }
    }

    private static void prune(Path directory) throws IOException
    {
        List<Snapshot> snapshots = listSnapshots(directory.getParent().getParent());
        List<Snapshot> matching = snapshots.stream()
                .filter(snapshot -> snapshot.archive().getParent().equals(directory)).toList();
        for (int index = MAX_SNAPSHOTS; index < matching.size(); index++)
        {
            Snapshot snapshot = matching.get(index);
            Files.deleteIfExists(snapshot.archive());
            String name = snapshot.archive().getFileName().toString();
            Files.deleteIfExists(snapshot.archive().resolveSibling(
                    name.substring(0, name.length() - 4) + ".properties"));
        }
    }

    private static void deleteSnapshot(Snapshot snapshot) throws IOException
    {
        Files.deleteIfExists(snapshot.archive());
        String name = snapshot.archive().getFileName().toString();
        Files.deleteIfExists(snapshot.archive().resolveSibling(
                name.substring(0, name.length() - 4) + ".properties"));
    }

    private static Path snapshotRoot(Path gameDirectory)
    {
        return gameDirectory.toAbsolutePath().normalize().resolve(SNAPSHOT_DIRECTORY);
    }

    private static void deleteRecursively(Path root) throws IOException
    {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root))
        {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList())
                Files.deleteIfExists(path);
        }
    }

    private static void showMessage(Minecraft minecraft, String message)
    {
        if (minecraft.player != null)
            minecraft.player.displayClientMessage(Component.literal(message), true);
    }

    private static String safeMessage(Exception exception)
    {
        return exception.getMessage() == null ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
