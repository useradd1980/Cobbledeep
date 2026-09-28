package dev.cobbledeep.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.slf4j.Logger;

/** Local-player-only opt-in preview. Never interferes with other players or the vanilla fallback. */
final class CobbledeepHumanoidPreview {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            "cobbledeep", "models/entity/humanoid_combat_idle.glb");
    // The exported base mesh uses a solid-color material. Reuse the existing
    // small white Cobbledeep texture rather than shipping a placeholder PNG.
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "cobbledeep", "textures/entity/loot_highlight_white.png");
    private static final boolean ENABLED = Boolean.parseBoolean(
            System.getProperty("cobbledeep.humanoidPreview", "false"));
    // Diagnostic only: freezes the evaluated GLB at the opening sample (Blender frame 1).
    // Set COBBLEDEEP_HUMANOID_FREEZE_FRAME1=true on the Gradle runClient process.
    // This does not change the player's actual position, rotation or game mechanics.
    private static final boolean FREEZE_FRAME_ONE = Boolean.parseBoolean(
            System.getenv("COBBLEDEEP_HUMANOID_FREEZE_FRAME1"));
    private static ResourceManager resourceManager;
    private static HumanoidGlbModel model;
    private static boolean failed;

    private CobbledeepHumanoidPreview() {}

    static boolean renderIfEnabled(AbstractClientPlayer player, float partialTick, PoseStack pose,
                                   MultiBufferSource buffers, int packedLight) {
        if (!ENABLED || player != Minecraft.getInstance().player) return false;
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        if (resourceManager != resources) {
            resourceManager = resources;
            model = null;
            failed = false;
        }
        if (failed) return false;
        if (model == null) {
            try {
                model = HumanoidGlbModel.load(resources, MODEL);
                if (FREEZE_FRAME_ONE) LOGGER.info("Cobbledeep humanoid diagnostic: GLB animation frozen at opening sample (Blender frame 1)");
            } catch (Exception e) {
                failed = true; // Missing asset or incompatible export: continue using vanilla renderer.
                LOGGER.warn("Cobbledeep humanoid preview disabled; vanilla player renderer remains active", e);
                return false;
            }
        }
        // These are the source export's evaluated glTF-world units; its 100x
        // Sketchfab ancestor is deliberately preserved to avoid rest-pose errors.
        // Frame-1 mesh height is ~118.5 source units, so 1/70 = 1.69 MC blocks.
        pose.pushPose();
        try {
            float yaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            // Blender-exported humanoid is already facing the correct way:
            // do not add the old 180-degree offset.
            pose.mulPose(new Quaternionf().rotationY(-yaw * ((float) Math.PI / 180f)));
            pose.scale(1f / 70f, 1f / 70f, 1f / 70f);
            VertexConsumer output = buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE_TEXTURE));
            float seconds = FREEZE_FRAME_ONE ? 0f : (player.tickCount + partialTick) / 20f;
            model.render(pose, output, packedLight, OverlayTexture.NO_OVERLAY, seconds, 0xFFD0C9BB);
            return true;
        } catch (RuntimeException e) {
            failed = true;
            LOGGER.warn("Cobbledeep humanoid preview render failed; reverting to vanilla renderer", e);
            return false;
        } finally {
            pose.popPose();
        }
    }
}
