package dev.cobbledeep.network;

import dev.cobbledeep.client.TacticalConsoleOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** A server-authored message displayed in the tactical console, not the vanilla HUD. */
public final class ConsoleMessagePacket {
    private final String text;
    private final TacticalConsoleOverlay.Category category;
    private final int rgb;

    public ConsoleMessagePacket(String text, TacticalConsoleOverlay.Category category, int rgb) {
        this.text = text == null ? "" : text;
        this.category = category == null ? TacticalConsoleOverlay.Category.SYSTEM : category;
        this.rgb = rgb;
    }

    public ConsoleMessagePacket(FriendlyByteBuf buffer) {
        this.text = buffer.readUtf(1024);
        int ordinal = buffer.readVarInt();
        TacticalConsoleOverlay.Category[] options = TacticalConsoleOverlay.Category.values();
        this.category = ordinal >= 0 && ordinal < options.length
                ? options[ordinal] : TacticalConsoleOverlay.Category.SYSTEM;
        this.rgb = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(text, 1024);
        buffer.writeVarInt(category.ordinal());
        buffer.writeInt(rgb);
    }

    public void handle(CustomPayloadEvent.Context context) {
        TacticalConsoleOverlay.addMessage(category, text, rgb);
    }
}
