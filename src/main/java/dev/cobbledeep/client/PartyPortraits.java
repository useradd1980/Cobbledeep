package dev.cobbledeep.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.character.CharacterCapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Client-local portrait art. A missing custom PNG always falls back to the player's skin. */
public final class PartyPortraits {
    public static final String DEFAULT = "Player skin";
    private static final long MAX_FILE_BYTES = 2L * 1024 * 1024;
    private static final int MAX_DIMENSION = 1024;
    private static final Map<String, Portrait> TEXTURES = new HashMap<>();
    private static final Properties CHOICES = new Properties();
    private static boolean choicesLoaded;

    private PartyPortraits() { }

    public static Path folder(Minecraft mc) {
        return mc.gameDirectory.toPath().resolve("cobbledeep").resolve("portraits");
    }

    private static Path preferences(Minecraft mc) {
        return mc.gameDirectory.toPath().resolve("config").resolve("cobbledeep-portraits.properties");
    }

    /** Only names from this directory are accepted; nothing outside it is opened. */
    public static List<String> available(Minecraft mc) {
        List<String> names = new ArrayList<>();
        try {
            Files.createDirectories(folder(mc));
            try (var files = Files.list(folder(mc))) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .filter(path -> {
                            try { return Files.size(path) <= MAX_FILE_BYTES; }
                            catch (IOException ignored) { return false; }
                        })
                        .map(path -> path.getFileName().toString())
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .limit(64)
                        .forEach(names::add);
            }
        } catch (IOException error) {
            Cobbledeep.LOGGER.warn("Could not list Cobbledeep portraits", error);
        }
        return names;
    }

    private static void loadChoices(Minecraft mc) {
        if (choicesLoaded) return;
        choicesLoaded = true;
        Path file = preferences(mc);
        if (!Files.isRegularFile(file)) return;
        try (InputStream input = Files.newInputStream(file)) {
            CHOICES.load(input);
        } catch (IOException error) {
            Cobbledeep.LOGGER.warn("Could not load Cobbledeep portrait selections", error);
        }
    }

    private static String characterKey(Minecraft mc) {
        if (mc.player == null) return "";
        String name = mc.player.getCapability(CharacterCapabilities.CHARACTER_DATA)
                .map(data -> data.isCharacterCreated() ? data.getName() : "")
                .orElse("");
        return mc.player.getUUID() + "/" + name;
    }

    public static String selected(Minecraft mc) {
        if (mc.player == null) return DEFAULT;
        loadChoices(mc);
        String chosen = CHOICES.getProperty(characterKey(mc), DEFAULT);
        return DEFAULT.equals(chosen) || available(mc).contains(chosen) ? chosen : DEFAULT;
    }

    public static void choose(Minecraft mc, String name) {
        if (mc.player == null || name == null) return;
        if (!DEFAULT.equals(name) && !available(mc).contains(name)) return;
        loadChoices(mc);
        CHOICES.setProperty(characterKey(mc), name);
        try {
            Files.createDirectories(preferences(mc).getParent());
            try (OutputStream output = Files.newOutputStream(preferences(mc))) {
                CHOICES.store(output, "Cobbledeep local character portrait selections");
            }
        } catch (IOException error) {
            Cobbledeep.LOGGER.warn("Could not save Cobbledeep portrait selection", error);
        }
    }

    /** Re-read edited images when the picker is opened or its Refresh button is used. */
    public static void refresh(Minecraft mc) {
        for (Portrait portrait : TEXTURES.values()) mc.getTextureManager().release(portrait.texture());
        TEXTURES.clear();
    }

    public static void draw(Minecraft mc, GuiGraphics graphics, String name,
                            int x, int y, int width, int height) {
        if (mc.player == null) return;
        if (!DEFAULT.equals(name)) {
            Portrait portrait = texture(mc, name);
            if (portrait != null) {
                graphics.blit(portrait.texture(), x, y, width, height, 0.0F, 0.0F,
                        portrait.width(), portrait.height(), portrait.width(), portrait.height());
                return;
            }
        }
        int size = Math.min(width, height);
        PlayerFaceRenderer.draw(graphics, mc.player.getSkin().texture(),
                x + (width - size) / 2, y + (height - size) / 2, size);
    }

    private static Portrait texture(Minecraft mc, String name) {
        Portrait existing = TEXTURES.get(name);
        if (existing != null) return existing;
        if (!available(mc).contains(name)) return null;
        Path file = folder(mc).resolve(name);
        try (InputStream input = Files.newInputStream(file)) {
            NativeImage image = NativeImage.read(input);
            if (image.getWidth() < 1 || image.getHeight() < 1
                    || image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
                image.close();
                return null;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = mc.getTextureManager().register("cobbledeep_portrait", texture);
            texture.upload();
            Portrait loaded = new Portrait(location, width, height);
            TEXTURES.put(name, loaded);
            return loaded;
        } catch (IOException | RuntimeException error) {
            Cobbledeep.LOGGER.warn("Unable to read Cobbledeep portrait '{}'", name, error);
            return null;
        }
    }

    private record Portrait(ResourceLocation texture, int width, int height) { }
}
