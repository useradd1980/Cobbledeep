package dev.cobbledeep.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.cobbledeep.Cobbledeep;
import dev.cobbledeep.exploration.TerrainRadius;
import dev.cobbledeep.exploration.FogVolume;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;

/** Depth-based world-space fog, composited before Minecraft draws the HUD. */
@Mod.EventBusSubscriber(modid = Cobbledeep.MODID, value = Dist.CLIENT)
public final class TacticalFogRenderer
{
    private static final byte[] PIXELS = new byte[FogVolume.LENGTH];
    private static ByteBuffer upload;
    private static int fogTexture = -1, depthTexture = -1;
    private static int depthWidth, depthHeight;
    private static int originX, originY, originZ;
    private static long lastTick = Long.MIN_VALUE;
    private static net.minecraft.client.multiplayer.ClientLevel lastLevel;

    private TacticalFogRenderer() { }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || !TacticalCameraController.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.isSpectator()) return;
        ShaderInstance shader = TacticalFogShaders.shader;
        if (shader == null) return;

        var target = mc.getMainRenderTarget();
        if (target.width <= 0 || target.height <= 0) return;
        Vec3 camera = event.getCamera().getPosition();
        int ox = Math.floorDiv(Mth.floor(camera.x / 2.0), 8) * 8 - FogVolume.SIZE / 2;
        int oy = Math.floorDiv(Mth.floor(camera.y / 2.0), 8) * 8 - FogVolume.SIZE / 2;
        int oz = Math.floorDiv(Mth.floor(camera.z / 2.0), 8) * 8 - FogVolume.SIZE / 2;
        long tick = mc.level.getGameTime();

        int oldActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int oldTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        int oldTexture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int equationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB), equationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        ShaderInstance oldShader = RenderSystem.getShader();
        try
        {
            boolean relocated = fogTexture == -1 || lastLevel != mc.level
                    || ox != originX || oy != originY || oz != originZ;
            // Refresh exploration memory five times per second. Current terrain
            // visibility follows the interpolated player position every frame.
            if (relocated || tick < lastTick || tick - lastTick >= 4)
            {
                originX = ox; originY = oy; originZ = oz;
                uploadFog(mc);
                lastTick = tick;
                lastLevel = mc.level;
            }

            // Copy depth into a separate texture: reading the attached depth
            // texture while drawing into the same framebuffer is undefined.
            target.bindWrite(false);
            if (depthTexture == -1) depthTexture = texture();
            RenderSystem.bindTexture(depthTexture);
            if (depthWidth != target.width || depthHeight != target.height)
            {
                GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT24,
                        0, 0, target.width, target.height, 0);
                depthWidth = target.width; depthHeight = target.height;
            }
            else GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, depthWidth, depthHeight);

            shader.setSampler("DepthSampler", depthTexture);
            shader.setSampler("FogSampler", fogTexture);
            shader.safeGetUniform("InverseProjection").set(new Matrix4f(event.getProjectionMatrix()).invert());
            shader.safeGetUniform("InverseView").set(new Matrix4f().rotation(event.getCamera().rotation()));
            shader.safeGetUniform("CameraPosition").set((float) camera.x, (float) camera.y, (float) camera.z);
            shader.safeGetUniform("GridOrigin").set((float) originX, (float) originY, (float) originZ);
            Vec3 player = mc.player.getPosition(event.getPartialTick());
            shader.safeGetUniform("PlayerPosition").set((float) player.x, (float) player.y, (float) player.z);
            shader.safeGetUniform("TerrainRange").set((float) TerrainRadius.RANGE);

            RenderSystem.disableDepthTest();
            RenderSystem.disableScissor();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(() -> shader);
            var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            builder.addVertex(-1, -1, 0).setUv(0, 0);
            builder.addVertex(1, -1, 0).setUv(1, 0);
            builder.addVertex(1, 1, 0).setUv(1, 1);
            builder.addVertex(-1, 1, 0).setUv(0, 1);
            BufferUploader.drawWithShader(builder.buildOrThrow());
        }
        finally
        {
            RenderSystem.depthMask(depthWrite);
            if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
            GlStateManager._blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            GL20.glBlendEquationSeparate(equationRgb, equationAlpha);
            if (scissor) GlStateManager._enableScissorTest(); else GlStateManager._disableScissorTest();
            RenderSystem.setShader(() -> oldShader);
            RenderSystem.activeTexture(GL13.GL_TEXTURE1);
            RenderSystem.bindTexture(oldTexture1);
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            RenderSystem.bindTexture(oldTexture);
            RenderSystem.activeTexture(oldActiveTexture);
        }
    }

    private static void uploadFog(Minecraft mc)
    {
        FogVolume.fill(PIXELS, ClientExploration.grid(mc.level.dimension().location()),
                originX, originY, originZ, (x, y, z) -> false);
        if (upload == null) upload = MemoryUtil.memAlloc(FogVolume.LENGTH);
        upload.clear(); upload.put(PIXELS); upload.flip();
        boolean created = fogTexture == -1;
        if (created) fogTexture = texture();
        RenderSystem.bindTexture(fogTexture);
        int alignment = GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
        int rowLength = GL11.glGetInteger(GL11.GL_UNPACK_ROW_LENGTH);
        int skipRows = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_ROWS);
        int skipPixels = GL11.glGetInteger(GL11.GL_UNPACK_SKIP_PIXELS);
        try
        {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
            if (created)
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_R8, FogVolume.WIDTH, FogVolume.HEIGHT,
                        0, GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, upload);
            else
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, FogVolume.WIDTH, FogVolume.HEIGHT,
                        GL11.GL_RED, GL11.GL_UNSIGNED_BYTE, upload);
        }
        finally
        {
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, alignment);
            GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, rowLength);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, skipRows);
            GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, skipPixels);
        }
    }

    private static int texture()
    {
        int id = GlStateManager._genTexture();
        RenderSystem.bindTexture(id);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL14.GL_TEXTURE_COMPARE_MODE, GL11.GL_NONE);
        return id;
    }

    public static void releaseTextures()
    {
        if (!RenderSystem.isOnRenderThread())
        {
            RenderSystem.recordRenderCall(TacticalFogRenderer::releaseTextures);
            return;
        }
        if (fogTexture != -1) GlStateManager._deleteTexture(fogTexture);
        if (depthTexture != -1) GlStateManager._deleteTexture(depthTexture);
        if (upload != null) MemoryUtil.memFree(upload);
        upload = null;
        fogTexture = depthTexture = -1;
        depthWidth = depthHeight = 0;
        lastTick = Long.MIN_VALUE;
        lastLevel = null;
    }
}
