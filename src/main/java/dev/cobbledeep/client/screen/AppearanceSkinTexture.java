package dev.cobbledeep.client.screen;

import com.mojang.blaze3d.platform.NativeImage;

import dev.cobbledeep.character.CharacterAppearance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** Builds a generated 64x64 Minecraft skin from Cobbledeep appearance choices. */
public final class AppearanceSkinTexture
{
    private static DynamicTexture texture;
    private static ResourceLocation textureLocation;
    private static String lastKey = "";

    private AppearanceSkinTexture() { }

    public static ResourceLocation get(CharacterAppearance appearance)
    {
        if (texture == null)
        {
            texture = new DynamicTexture(64, 64, false);
            textureLocation = Minecraft.getInstance().getTextureManager()
                    .register("cobbledeep_appearance_preview", texture);
        }
        String key = buildKey(appearance);
        if (!key.equals(lastKey))
        {
            rebuild(appearance);
            lastKey = key;
        }
        return textureLocation;
    }

    private static String buildKey(CharacterAppearance a)
    {
        if (a == null) return "default";
        return a.getSkinTone()+"|"+a.getHairStyle()+"|"+a.getHairColor()+"|"+a.getEyeColor()+"|"
                +a.getFacialHair()+"|"+a.getShirtColor()+"|"+a.getTrouserColor();
    }

    private static void rebuild(CharacterAppearance a)
    {
        NativeImage p = texture.getPixels();
        if (p == null || a == null) return;
        clear(p);
        int skin=a.getSkinTone().getRgb(), shirt=a.getShirtColor().getRgb(), trousers=a.getTrouserColor().getRgb();
        int hair=a.getHairColor().getRgb(), eyes=a.getEyeColor().getRgb();
        paintHead(p,skin); paintTorso(p,shirt); paintRightArm(p,skin,shirt); paintLeftArm(p,skin,shirt);
        paintRightLeg(p,trousers); paintLeftLeg(p,trousers);
        paintFace(p,skin,hair,eyes,a.getHairStyle());
        paintFacialHair(p,hair,a.getFacialHair());
        paintMouth(p,skin,a.getFacialHair());
        paintHair(p,hair,a.getHairStyle());
        texture.upload();
    }

    private static void clear(NativeImage i){ for(int y=0;y<64;y++) for(int x=0;x<64;x++) i.setPixelRGBA(x,y,0); }

    private static void paintHead(NativeImage i,int c){ paintBoxFaces(i,c,8,0,8,8,16,0,8,8,0,8,8,8,8,8,8,8,16,8,8,8,24,8,8,8,true); }
    private static void paintTorso(NativeImage i,int c){ paintBoxFaces(i,c,20,16,8,4,28,16,8,4,16,20,4,12,20,20,8,12,28,20,4,12,32,20,8,12,false); }
    private static void paintRightLeg(NativeImage i,int c){ paintBoxFaces(i,c,4,16,4,4,8,16,4,4,0,20,4,12,4,20,4,12,8,20,4,12,12,20,4,12,false); }
    private static void paintLeftLeg(NativeImage i,int c){ paintBoxFaces(i,c,20,48,4,4,24,48,4,4,16,52,4,12,20,52,4,12,24,52,4,12,28,52,4,12,false); }

    private static void paintRightArm(NativeImage i,int skin,int shirt)
    {
        paintBoxFaces(i,skin,44,16,4,4,48,16,4,4,40,20,4,12,44,20,4,12,48,20,4,12,52,20,4,12,true);
        paintBlotchyRect(i,40,20,4,4,shirt,.92F,31,.08F); paintBlotchyRect(i,44,20,4,4,shirt,1F,37,.08F);
        paintBlotchyRect(i,48,20,4,4,shirt,.86F,41,.08F); paintBlotchyRect(i,52,20,4,4,shirt,.82F,43,.08F);
        paintBlotchyRect(i,44,16,4,4,shirt,1.05F,47,.08F);
    }
    private static void paintLeftArm(NativeImage i,int skin,int shirt)
    {
        paintBoxFaces(i,skin,36,48,4,4,40,48,4,4,32,52,4,12,36,52,4,12,40,52,4,12,44,52,4,12,true);
        paintBlotchyRect(i,32,52,4,4,shirt,.92F,53,.08F); paintBlotchyRect(i,36,52,4,4,shirt,1F,59,.08F);
        paintBlotchyRect(i,40,52,4,4,shirt,.86F,61,.08F); paintBlotchyRect(i,44,52,4,4,shirt,.82F,67,.08F);
        paintBlotchyRect(i,36,48,4,4,shirt,1.05F,71,.08F);
    }

    private static void paintFace(NativeImage i,int skin,int hair,int eyes,CharacterAppearance.HairStyle style)
    {
        int white=0xE8E8E8;
        pixel(i,9,11,white); pixel(i,10,11,eyes); pixel(i,13,11,eyes); pixel(i,14,11,white);
        pixel(i,11,13,shade(skin,.92F)); pixel(i,12,13,shade(skin,.92F));
        pixel(i,11,14,shade(skin,.86F)); pixel(i,12,14,shade(skin,.86F));
        if(style!=CharacterAppearance.HairStyle.BALD){ int h=style==CharacterAppearance.HairStyle.CROPPED?2:3;
            paintBlotchyRect(i,8,8,8,h,hair,1F,79,.08F);
            if(style!=CharacterAppearance.HairStyle.CROPPED){ paintBlotchyRect(i,8,10,1,3,hair,.86F,83,.06F); paintBlotchyRect(i,15,10,1,3,hair,.90F,89,.06F); }
        }
    }

    private static void paintFacialHair(NativeImage i,int hair,CharacterAppearance.FacialHair style)
    {
        if(style==null || style==CharacterAppearance.FacialHair.NONE) return;
        int dark=shade(hair,.78F), light=shade(hair,.94F);
        switch(style)
        {
            case STUBBLE -> { pixel(i,9,14,dark); pixel(i,11,15,dark); pixel(i,13,14,dark); pixel(i,14,15,dark); }
            case MOUSTACHE -> { pixel(i,10,14,dark); pixel(i,13,14,dark); pixel(i,10,15,light); pixel(i,13,15,light); }
            case GOATEE -> { pixel(i,10,14,dark); pixel(i,13,14,dark); paintRect(i,11,15,2,1,hair); }
            case SHORT_BEARD -> { paintRect(i,9,15,6,1,hair); pixel(i,9,14,dark); pixel(i,14,14,dark); pixel(i,10,14,light); pixel(i,13,14,light); }
            case FULL_BEARD -> { paintRect(i,8,13,1,3,dark); paintRect(i,15,13,1,3,dark); paintRect(i,9,15,6,1,hair); pixel(i,9,14,hair); pixel(i,14,14,hair); }
            default -> { }
        }
    }

    private static void paintMouth(NativeImage i,int skin,CharacterAppearance.FacialHair style)
    {
        int mouth = shade(skin, .58F);
        if (style == null || style == CharacterAppearance.FacialHair.NONE)
        {
            pixel(i,11,14,mouth); pixel(i,12,14,mouth);
            return;
        }
        // Keep a clear two-pixel mouth opening even under moustaches and beards.
        pixel(i,11,14,mouth); pixel(i,12,14,mouth);
        pixel(i,11,15,shade(skin,.82F)); pixel(i,12,15,shade(skin,.82F));
    }

    private static void paintHair(NativeImage i,int hair,CharacterAppearance.HairStyle style)
    {
        if(style==CharacterAppearance.HairStyle.BALD) return;
        paintBlotchyRect(i,8,0,8,8,hair,1F,97,.09F);
        int d=style==CharacterAppearance.HairStyle.CROPPED?2:style==CharacterAppearance.HairStyle.SHORT?4:7;
        paintBlotchyRect(i,0,8,8,d,hair,.88F,101,.08F); paintBlotchyRect(i,16,8,8,d,hair,.92F,103,.08F);
        paintBlotchyRect(i,24,8,8,Math.max(4,d),hair,.82F,107,.09F);
        // Longer styles continue below the head as model geometry. Do not paint
        // hair into the torso's skin atlas, or it reads as colour bleeding into clothing.
    }

    private static void paintBoxFaces(NativeImage i,int c,int tx,int ty,int tw,int th,int bx,int by,int bw,int bh,
            int rx,int ry,int rw,int rh,int fx,int fy,int fw,int fh,int lx,int ly,int lw,int lh,int kx,int ky,int kw,int kh,boolean subtle)
    {
        float v=subtle?.045F:.10F;
        paintBlotchyRect(i,tx,ty,tw,th,c,1.04F,tx*3+ty,v); paintBlotchyRect(i,bx,by,bw,bh,c,.82F,bx*3+by,v);
        paintBlotchyRect(i,rx,ry,rw,rh,c,.95F,rx*3+ry,v); paintBlotchyRect(i,fx,fy,fw,fh,c,1F,fx*3+fy,v);
        paintBlotchyRect(i,lx,ly,lw,lh,c,.90F,lx*3+ly,v); paintBlotchyRect(i,kx,ky,kw,kh,c,.86F,kx*3+ky,v);
    }

    private static void paintBlotchyRect(NativeImage i,int x,int y,int w,int h,int c,float base,int seed,float amount)
    {
        for(int py=y;py<y+h;py++) for(int px=x;px<x+w;px++){
            int hash=px*73428767 ^ py*912931 ^ seed*19349663; hash^=hash>>>13; int b=Math.floorMod(hash,11);
            float unit=switch(b){case 0->-1F; case 1,2->-.55F; case 8,9->.5F; case 10->.9F; default->0F;};
            pixel(i,px,py,shade(c,base+unit*amount));
        }
    }
    private static void paintRect(NativeImage i,int x,int y,int w,int h,int c){ for(int py=y;py<y+h;py++) for(int px=x;px<x+w;px++) pixel(i,px,py,c); }
    private static void pixel(NativeImage i,int x,int y,int c){ i.setPixelRGBA(x,y,toAbgr(c)); }
    private static int shade(int c,float f){ int r=Math.min(255,Math.max(0,Math.round(((c>>16)&255)*f))),g=Math.min(255,Math.max(0,Math.round(((c>>8)&255)*f))),b=Math.min(255,Math.max(0,Math.round((c&255)*f))); return r<<16|g<<8|b; }
    private static int toAbgr(int c){ int r=c>>16&255,g=c>>8&255,b=c&255; return 0xFF000000|b<<16|g<<8|r; }
}
