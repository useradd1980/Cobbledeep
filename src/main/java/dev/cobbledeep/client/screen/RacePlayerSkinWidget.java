package dev.cobbledeep.client.screen;

import java.util.function.Supplier;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.cobbledeep.character.CharacterAppearance;
import dev.cobbledeep.character.CharacterRace;
import dev.cobbledeep.character.PendingCharacter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerSkinWidget;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RacePlayerSkinWidget extends PlayerSkinWidget
{
    private static final float ROTATION_SENSITIVITY=2.5F, ROTATION_X_LIMIT=50F, DEFAULT_ROTATION_X=-5F, DEFAULT_ROTATION_Y=30F;
    private static final float MODEL_OFFSET=.0625F, MODEL_HEIGHT=2.125F, Z_OFFSET=100F, ROTATION_PIVOT_Y=-1.0625F, MODEL_TRANSLATE_Y=-1.5F;
    private static final float ELF_EAR_UP_ANGLE=32F, HALF_ELF_EAR_UP_ANGLE=22F, ELF_EAR_BACK_ANGLE=30F, HALF_ELF_EAR_BACK_ANGLE=20F;
    private static final int DEFAULT_PREVIEW_SKIN_COLOR=0xFFB47A60, DEFAULT_DWARF_BEARD_COLOR=0xFF5B3825, DWARF_TUNIC_COLOR=0xFF3E6F6A;
    private static ResourceLocation whiteTexture;
    private static float savedRotationX=DEFAULT_ROTATION_X,savedRotationY=DEFAULT_ROTATION_Y;
    private final Supplier<CharacterRace> raceSupplier; private final Supplier<CharacterAppearance> appearanceSupplier; private final Supplier<PendingCharacter.Gender> genderSupplier;
    private final ModelPart elfEars,halfElfEars,dwarfMaleBuild,dwarfFemaleBuild,dwarfArms,dwarfBeard,dwarfBeardHighlights,dwarfBeardShadows,longHairBridge,braidedHairBridge;
    private float previewRotationX=savedRotationX,previewRotationY=savedRotationY;

    public RacePlayerSkinWidget(int w,int h,EntityModelSet m,Supplier<PlayerSkin>s,Supplier<CharacterRace>r){this(w,h,m,s,r,()->null,()->PendingCharacter.Gender.MALE);}
    public RacePlayerSkinWidget(int w,int h,EntityModelSet m,Supplier<PlayerSkin>s,Supplier<CharacterRace>r,Supplier<CharacterAppearance>a){this(w,h,m,s,r,a,()->PendingCharacter.Gender.MALE);}
    public RacePlayerSkinWidget(int w,int h,EntityModelSet m,Supplier<PlayerSkin>s,Supplier<CharacterRace>r,Supplier<CharacterAppearance>a,Supplier<PendingCharacter.Gender>g)
    {
        super(w,h,m,()->buildAppearanceSkin(s,a,g)); raceSupplier=r;appearanceSupplier=a;genderSupplier=g;
        elfEars=createEars(false);halfElfEars=createEars(true);dwarfMaleBuild=createDwarfMaleBuild();dwarfFemaleBuild=createDwarfFemaleBuild();dwarfArms=createDwarfArms();
        dwarfBeard=createDwarfBeard();dwarfBeardHighlights=createDwarfBeardHighlights();dwarfBeardShadows=createDwarfBeardShadows();longHairBridge=createHairBridge(false);braidedHairBridge=createHairBridge(true);
        float dx=(savedRotationY-DEFAULT_ROTATION_Y)/ROTATION_SENSITIVITY,dy=(DEFAULT_ROTATION_X-savedRotationX)/ROTATION_SENSITIVITY;if(dx!=0||dy!=0)super.onDrag(0,0,dx,dy);
        previewRotationX=savedRotationX;previewRotationY=savedRotationY;
    }
    private static PlayerSkin buildAppearanceSkin(Supplier<PlayerSkin>f,Supplier<CharacterAppearance>a,Supplier<PendingCharacter.Gender>g){PlayerSkin p=f.get();CharacterAppearance x=a.get();if(x==null)return p;return new PlayerSkin(AppearanceSkinTexture.get(x),p.textureUrl(),p.capeTexture(),p.elytraTexture(),g.get()==PendingCharacter.Gender.FEMALE?PlayerSkin.Model.SLIM:PlayerSkin.Model.WIDE,false);}
    @Override protected void renderWidget(GuiGraphics q,int mx,int my,float pt){CharacterRace r=raceSupplier.get();RaceScale s=getRaceScale(r);float cx=getX()+getWidth()/2F,fy=getY()+getHeight();q.pose().pushPose();q.pose().translate(cx,fy,0);q.pose().scale(s.widthScale(),s.heightScale(),1);q.pose().translate(-cx,-fy,0);super.renderWidget(q,mx,my,pt);renderRaceGeometry(q,r);q.pose().popPose();}
    @Override protected void onDrag(double x,double y,double dx,double dy){super.onDrag(x,y,dx,dy);previewRotationX=Mth.clamp(previewRotationX-(float)dy*ROTATION_SENSITIVITY,-ROTATION_X_LIMIT,ROTATION_X_LIMIT);previewRotationY+=(float)dx*ROTATION_SENSITIVITY;savedRotationX=previewRotationX;savedRotationY=previewRotationY;}

    private void renderRaceGeometry(GuiGraphics q,CharacterRace race){CharacterAppearance a=appearanceSupplier.get();boolean hair=hasLongHair(a),geo=race==CharacterRace.ELF||race==CharacterRace.HALF_ELF||race==CharacterRace.DWARF;if(!geo&&!hair)return;PoseStack p=q.pose();p.pushPose();p.translate(getX()+getWidth()/2F,getY()+getHeight(),Z_OFFSET);float sc=getHeight()/MODEL_HEIGHT;p.scale(sc,sc,sc);p.translate(0,-MODEL_OFFSET,0);p.translate(0,ROTATION_PIVOT_Y,0);p.mulPose(Axis.XP.rotationDegrees(previewRotationX));p.translate(0,-ROTATION_PIVOT_Y,0);p.mulPose(Axis.YP.rotationDegrees(previewRotationY));p.scale(1,1,-1);p.translate(0,MODEL_TRANSLATE_Y,0);
        if(race==CharacterRace.DWARF){renderDwarfBuild(q,p);if(genderSupplier.get()!=PendingCharacter.Gender.FEMALE)renderDwarfBeard(q,p);}else if(race==CharacterRace.ELF||race==CharacterRace.HALF_ELF)renderElvenEars(q,p,race);if(hair)renderHairBridge(q,p,a);q.flush();p.popPose();}
    private static boolean hasLongHair(CharacterAppearance a){if(a==null||a.getHairStyle()==null)return false;return a.getHairStyle()==CharacterAppearance.HairStyle.SHOULDER_LENGTH||a.getHairStyle()==CharacterAppearance.HairStyle.LONG||a.getHairStyle()==CharacterAppearance.HairStyle.BRAIDED;}
    private void renderHairBridge(GuiGraphics q,PoseStack p,CharacterAppearance a){int c=0xFF3B261B;if(a!=null&&a.getHairColor()!=null)c=0xFF000000|a.getHairColor().getRgb();ModelPart b=a!=null&&a.getHairStyle()==CharacterAppearance.HairStyle.BRAIDED?braidedHairBridge:longHairBridge;b.render(p,q.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,c);}
    private void renderElvenEars(GuiGraphics q,PoseStack p,CharacterRace r){ModelPart e=r==CharacterRace.ELF?elfEars:halfElfEars;CharacterAppearance a=appearanceSupplier.get();int c=DEFAULT_PREVIEW_SKIN_COLOR;if(a!=null&&a.getSkinTone()!=null)c=0xFF000000|a.getSkinTone().getRgb();e.render(p,q.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,c);}
    private void renderDwarfBuild(GuiGraphics q,PoseStack p){CharacterAppearance a=appearanceSupplier.get();int c=DWARF_TUNIC_COLOR;if(a!=null&&a.getShirtColor()!=null)c=0xFF000000|a.getShirtColor().getRgb();ModelPart t=genderSupplier.get()==PendingCharacter.Gender.FEMALE?dwarfFemaleBuild:dwarfMaleBuild;t.render(p,q.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,c);dwarfArms.render(p,q.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,c);}
    private void renderDwarfBeard(GuiGraphics q,PoseStack p){CharacterAppearance a=appearanceSupplier.get();int c=DEFAULT_DWARF_BEARD_COLOR;if(a!=null&&a.getHairColor()!=null)c=0xFF000000|a.getHairColor().getRgb();int sh=scaleRgb(c,.58F),hi=scaleRgb(c,1.35F);dwarfBeardShadows.render(p,q.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,sh);dwarfBeard.render(p,q.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,c);dwarfBeardHighlights.render(p,q.bufferSource().getBuffer(RenderType.entityCutoutNoCull(getWhiteTexture())),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,hi);}
    private static int scaleRgb(int c,float f){int r=Math.min(255,Math.round(((c>>16)&255)*f)),g=Math.min(255,Math.round(((c>>8)&255)*f)),b=Math.min(255,Math.round((c&255)*f));return 0xFF000000|r<<16|g<<8|b;}
    private static ResourceLocation getWhiteTexture(){if(whiteTexture==null){DynamicTexture t=new DynamicTexture(1,1,false);NativeImage p=t.getPixels();if(p!=null){p.setPixelRGBA(0,0,0xFFFFFFFF);t.upload();}whiteTexture=Minecraft.getInstance().getTextureManager().register("cobbledeep_preview_white",t);}return whiteTexture;}

    private static ModelPart createHairBridge(boolean braided){MeshDefinition m=new MeshDefinition();float w=braided?3.0F:6.5F,x=-w/2F;CubeListBuilder b=CubeListBuilder.create()
            .texOffs(0,0).addBox(x,-1.15F,3.55F,w,2.55F,.70F)
            .texOffs(0,0).addBox(x,-.15F,2.75F,w,1.80F,1.05F);
        m.getRoot().addOrReplaceChild(braided?"braided_hair_bridge":"long_hair_bridge",b,PartPose.ZERO);return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart createDwarfMaleBuild(){MeshDefinition m=new MeshDefinition();CubeListBuilder b=CubeListBuilder.create().texOffs(0,0).addBox(-4.75F,.25F,-2.30F,9.5F,4.25F,4.6F).texOffs(0,0).addBox(-4.45F,4.5F,-2.2F,8.9F,4F,4.4F).texOffs(0,0).addBox(-4.2F,8.5F,-2.1F,8.4F,3.25F,4.2F);m.getRoot().addOrReplaceChild("dwarf_male_torso",b,PartPose.ZERO);return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart createDwarfFemaleBuild(){MeshDefinition m=new MeshDefinition();CubeListBuilder b=CubeListBuilder.create().texOffs(0,0).addBox(-4.5F,.35F,-2.25F,9F,4F,4.5F).texOffs(0,0).addBox(-4.25F,4.35F,-2.18F,8.5F,4.1F,4.36F).texOffs(0,0).addBox(-4.15F,8.45F,-2.12F,8.3F,3.25F,4.24F);m.getRoot().addOrReplaceChild("dwarf_female_torso",b,PartPose.ZERO);return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart createDwarfArms(){MeshDefinition m=new MeshDefinition();CubeListBuilder b=CubeListBuilder.create().texOffs(0,0).addBox(-7.1F,1.1F,-2.15F,2.45F,5.35F,4.3F).texOffs(0,0).addBox(4.65F,1.1F,-2.15F,2.45F,5.35F,4.3F);m.getRoot().addOrReplaceChild("dwarf_upper_arms",b,PartPose.ZERO);return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart createDwarfBeard(){MeshDefinition m=new MeshDefinition();CubeListBuilder b=CubeListBuilder.create().texOffs(0,0).addBox(-3.35F,-2.55F,-4.65F,6.7F,1.2F,.85F).texOffs(0,0).addBox(-3.65F,-1.45F,-4.55F,7.3F,2.2F,.95F).texOffs(0,0).addBox(-3.2F,.6F,-3.9F,6.4F,2.35F,1.25F).texOffs(0,0).addBox(-2.65F,2.75F,-3.55F,5.3F,2.2F,1.2F).texOffs(0,0).addBox(-1.8F,4.75F,-3.25F,3.6F,1.45F,1.05F);m.getRoot().addOrReplaceChild("dwarf_beard",b,PartPose.ZERO);return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart createDwarfBeardHighlights(){MeshDefinition m=new MeshDefinition();CubeListBuilder b=CubeListBuilder.create().texOffs(0,0).addBox(-1.45F,-2.45F,-4.78F,2.9F,.55F,.24F).texOffs(0,0).addBox(-1.65F,-1.15F,-4.68F,3.3F,.85F,.24F).texOffs(0,0).addBox(-2.25F,.9F,-4.02F,1.25F,3.4F,.26F).texOffs(0,0).addBox(1F,.9F,-4.02F,1.25F,3.4F,.26F).texOffs(0,0).addBox(-.7F,4.95F,-3.37F,1.4F,.85F,.22F);m.getRoot().addOrReplaceChild("dwarf_beard_highlights",b,PartPose.ZERO);return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart createDwarfBeardShadows(){MeshDefinition m=new MeshDefinition();CubeListBuilder b=CubeListBuilder.create().texOffs(0,0).addBox(-3.58F,-1.2F,-4.67F,.7F,2.05F,.28F).texOffs(0,0).addBox(2.88F,-1.2F,-4.67F,.7F,2.05F,.28F).texOffs(0,0).addBox(-2.95F,2.35F,-3.7F,.75F,2.35F,.3F).texOffs(0,0).addBox(2.2F,2.35F,-3.7F,.75F,2.35F,.3F).texOffs(0,0).addBox(-1.65F,5.75F,-3.37F,3.3F,.38F,.24F);m.getRoot().addOrReplaceChild("dwarf_beard_shadows",b,PartPose.ZERO);return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart createEars(boolean half){MeshDefinition m=new MeshDefinition();float up=(float)Math.toRadians(half?HALF_ELF_EAR_UP_ANGLE:ELF_EAR_UP_ANGLE),back=(float)Math.toRadians(half?HALF_ELF_EAR_BACK_ANGLE:ELF_EAR_BACK_ANGLE);CubeListBuilder l=CubeListBuilder.create(),r=CubeListBuilder.create();if(half){l.texOffs(0,0).addBox(-1.2F,-.75F,-.55F,1.2F,1.5F,1.1F).texOffs(0,0).addBox(-2.1F,-.5F,-.4F,.9F,1F,.8F).texOffs(0,0).addBox(-2.55F,-.25F,-.25F,.45F,.5F,.5F);r.texOffs(0,0).addBox(0,-.75F,-.55F,1.2F,1.5F,1.1F).texOffs(0,0).addBox(1.2F,-.5F,-.4F,.9F,1F,.8F).texOffs(0,0).addBox(2.1F,-.25F,-.25F,.45F,.5F,.5F);}else{l.texOffs(0,0).addBox(-1.45F,-.9F,-.65F,1.45F,1.8F,1.3F).texOffs(0,0).addBox(-2.65F,-.65F,-.5F,1.2F,1.3F,1F).texOffs(0,0).addBox(-3.45F,-.4F,-.35F,.8F,.8F,.7F).texOffs(0,0).addBox(-3.85F,-.2F,-.2F,.4F,.4F,.4F);r.texOffs(0,0).addBox(0,-.9F,-.65F,1.45F,1.8F,1.3F).texOffs(0,0).addBox(1.45F,-.65F,-.5F,1.2F,1.3F,1F).texOffs(0,0).addBox(2.65F,-.4F,-.35F,.8F,.8F,.7F).texOffs(0,0).addBox(3.45F,-.2F,-.2F,.4F,.4F,.4F);}m.getRoot().addOrReplaceChild("left_ear",l,PartPose.offsetAndRotation(-4,-4,0,0,back,up));m.getRoot().addOrReplaceChild("right_ear",r,PartPose.offsetAndRotation(4,-4,0,0,-back,-up));return LayerDefinition.create(m,16,16).bakeRoot();}
    private static RaceScale getRaceScale(CharacterRace r){if(r==null)return RaceScale.HUMAN;return switch(r){case HUMAN->RaceScale.HUMAN;case ELF->new RaceScale(.90F,1.06F);case HALF_ELF->new RaceScale(.96F,1.02F);case DWARF->new RaceScale(1.22F,.78F);case HALFLING->new RaceScale(.84F,.68F);case GNOME->new RaceScale(.90F,.72F);};}
    private record RaceScale(float widthScale,float heightScale){private static final RaceScale HUMAN=new RaceScale(1,1);}
}
