package me.bewf.hitspan.util;

import net.minecraft.client.Minecraft;
import net.minecraft.text.LiteralText;

public final class Chat {

    private Chat() {}

    public static void send(String legacyText) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        mc.player.sendMessage(new LiteralText(legacyText));
    }
}
