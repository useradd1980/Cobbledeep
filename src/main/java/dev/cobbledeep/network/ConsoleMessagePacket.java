package dev.cobbledeep.network;

import dev.cobbledeep.client.TacticalConsoleOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Server-authored console message; server logic never has to load a client UI class. */
public final class ConsoleMessagePacket {
    public static final int COMBAT = 0;
    public static final int DIALOGUE = 1;
    public static final int SYSTEM = 2;

    private final String text;
    private final int category;
    private final int rgb;

    public ConsoleMessagePacket(String text, int category, int rgb) {
        this.text = text == null ? "" : text;
        this.category = category >= COMBAT && category <= SYSTEM ? category : SYSTEM;
        this.rgb = rgb;
    }

    public ConsoleMessagePacket(FriendlyByteBuf buffer) {
        this.text = buffer.readUtf(1024);
        int receivedCategory = buffer.readVarInt();
        this.category = receivedCategory >= COMBAT && receivedCategory <= SYSTEM
                ? receivedCategory : SYSTEM;
        this.rgb = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(text, 1024);
        buffer.writeVarInt(category);
        buffer.writeInt(rgb);
    }

    public void handle(CustomPayloadEvent.Context context) {
        TacticalConsoleOverlay.Category[] categories = TacticalConsoleOverlay.Category.values();
        TacticalConsoleOverlay.addMessage(categories[category], text, rgb);
    }
}
