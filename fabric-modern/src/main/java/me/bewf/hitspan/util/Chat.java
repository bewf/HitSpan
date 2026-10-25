package me.bewf.hitspan.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Chat {

    private Chat() {}

    public static void send(String legacyText) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        mc.execute(() -> sendNow(legacyText));
    }

    private static void sendNow(String legacyText) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;

        mc.player.sendSystemMessage(Component.literal(legacyText));
    }

    public static String minecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
