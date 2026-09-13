package dev.cobbledeep.character;

/**
 * Semantic appearance choices selected during character generation.
 *
 * These values deliberately describe what the character should look like
 * rather than tying creation data to a particular skin texture. A renderer can
 * later translate them into layered textures, model parts, or generated skins.
 */
public class CharacterAppearance
{
    public enum SkinTone
    {
        PALE("Pale", 0xF6D0B1),
        FAIR("Fair", 0xE8B58F),
        MEDIUM("Medium", 0xC98D66),
        TAN("Tan", 0xA96D4E),
        BROWN("Brown", 0x80543C),
        DEEP("Deep", 0x5A382A);

        private final String displayName;
        private final int rgb;

        SkinTone(String displayName, int rgb)
        {
            this.displayName = displayName;
            this.rgb = rgb;
        }

        public String getDisplayName() { return displayName; }
        public int getRgb() { return rgb; }
    }

    public enum HairStyle
    {
        BALD("Bald"),
        CROPPED("Cropped"),
        SHORT("Short"),
        SHOULDER_LENGTH("Shoulder Length"),
        LONG("Long"),
        BRAIDED("Braided");

        private final String displayName;

        HairStyle(String displayName)
        {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    public enum HairColor
    {
        BLACK("Black", 0x1B1715),
        DARK_BROWN("Dark Brown", 0x3B261B),
        BROWN("Brown", 0x6A4026),
        AUBURN("Auburn", 0x7A3020),
        BLONDE("Blonde", 0xD2B36D),
        GREY("Grey", 0x8C8C88),
        WHITE("White", 0xD8D8D2);

        private final String displayName;
        private final int rgb;

        HairColor(String displayName, int rgb)
        {
            this.displayName = displayName;
            this.rgb = rgb;
        }

        public String getDisplayName() { return displayName; }
        public int getRgb() { return rgb; }
    }

    public enum EyeColor
    {
        BROWN("Brown", 0x5C3A24),
        HAZEL("Hazel", 0x80652E),
        GREEN("Green", 0x46683F),
        BLUE("Blue", 0x416F99),
        GREY("Grey", 0x707A80);

        private final String displayName;
        private final int rgb;

        EyeColor(String displayName, int rgb)
        {
            this.displayName = displayName;
            this.rgb = rgb;
        }

        public String getDisplayName() { return displayName; }
        public int getRgb() { return rgb; }
    }

    public enum FacialHair
    {
        NONE("None"),
        STUBBLE("Stubble"),
        MOUSTACHE("Moustache"),
        GOATEE("Goatee"),
        SHORT_BEARD("Short Beard"),
        FULL_BEARD("Full Beard");

        private final String displayName;

        FacialHair(String displayName)
        {
            this.displayName = displayName;
        }

        public String getDisplayName() { return displayName; }
    }

    public enum ClothingColor
    {
        BLACK("Black", 0x242424),
        CHARCOAL("Charcoal", 0x444444),
        BROWN("Brown", 0x70482F),
        TAN("Tan", 0xB58A5A),
        RED("Red", 0x9E3434),
        BURGUNDY("Burgundy", 0x6E2637),
        ORANGE("Orange", 0xB8642D),
        YELLOW("Yellow", 0xC7A83A),
        GREEN("Green", 0x4F753D),
        TEAL("Teal", 0x34766F),
        BLUE("Blue", 0x365F9A),
        NAVY("Navy", 0x2D3E67),
        PURPLE("Purple", 0x694D82),
        GREY("Grey", 0x777777),
        WHITE("White", 0xD8D8D2);

        private final String displayName;
        private final int rgb;

        ClothingColor(String displayName, int rgb)
        {
            this.displayName = displayName;
            this.rgb = rgb;
        }

        public String getDisplayName() { return displayName; }
        public int getRgb() { return rgb; }
    }

    private SkinTone skinTone;
    private HairStyle hairStyle;
    private HairColor hairColor;
    private EyeColor eyeColor;
    private FacialHair facialHair;
    private ClothingColor shirtColor;
    private ClothingColor trouserColor;

    public CharacterAppearance()
    {
        reset();
    }

    public void reset()
    {
        skinTone = SkinTone.FAIR;
        hairStyle = HairStyle.SHORT;
        hairColor = HairColor.DARK_BROWN;
        eyeColor = EyeColor.BROWN;
        facialHair = FacialHair.NONE;
        shirtColor = ClothingColor.TEAL;
        trouserColor = ClothingColor.NAVY;
    }

    public SkinTone getSkinTone() { return skinTone; }
    public void setSkinTone(SkinTone skinTone) { this.skinTone = skinTone; }
    public void previousSkinTone() { skinTone = previous(SkinTone.values(), skinTone); }
    public void nextSkinTone() { skinTone = next(SkinTone.values(), skinTone); }

    public HairStyle getHairStyle() { return hairStyle; }
    public void setHairStyle(HairStyle hairStyle) { this.hairStyle = hairStyle; }
    public void previousHairStyle() { hairStyle = previous(HairStyle.values(), hairStyle); }
    public void nextHairStyle() { hairStyle = next(HairStyle.values(), hairStyle); }

    public HairColor getHairColor() { return hairColor; }
    public void setHairColor(HairColor hairColor) { this.hairColor = hairColor; }
    public void previousHairColor() { hairColor = previous(HairColor.values(), hairColor); }
    public void nextHairColor() { hairColor = next(HairColor.values(), hairColor); }

    public EyeColor getEyeColor() { return eyeColor; }
    public void setEyeColor(EyeColor eyeColor) { this.eyeColor = eyeColor; }
    public void previousEyeColor() { eyeColor = previous(EyeColor.values(), eyeColor); }
    public void nextEyeColor() { eyeColor = next(EyeColor.values(), eyeColor); }

    public FacialHair getFacialHair() { return facialHair; }
    public void setFacialHair(FacialHair facialHair) { this.facialHair = facialHair; }
    public void previousFacialHair() { facialHair = previous(FacialHair.values(), facialHair); }
    public void nextFacialHair() { facialHair = next(FacialHair.values(), facialHair); }

    public ClothingColor getShirtColor() { return shirtColor; }
    public void setShirtColor(ClothingColor shirtColor) { this.shirtColor = shirtColor; }
    public void previousShirtColor() { shirtColor = previous(ClothingColor.values(), shirtColor); }
    public void nextShirtColor() { shirtColor = next(ClothingColor.values(), shirtColor); }

    public ClothingColor getTrouserColor() { return trouserColor; }
    public void setTrouserColor(ClothingColor trouserColor) { this.trouserColor = trouserColor; }
    public void previousTrouserColor() { trouserColor = previous(ClothingColor.values(), trouserColor); }
    public void nextTrouserColor() { trouserColor = next(ClothingColor.values(), trouserColor); }

    private static <T> T previous(T[] values, T current)
    {
        int index = indexOf(values, current);
        return values[(index - 1 + values.length) % values.length];
    }

    private static <T> T next(T[] values, T current)
    {
        int index = indexOf(values, current);
        return values[(index + 1) % values.length];
    }

    private static <T> int indexOf(T[] values, T current)
    {
        for (int i = 0; i < values.length; i++)
        {
            if (values[i] == current)
            {
                return i;
            }
        }
        return 0;
    }
}
