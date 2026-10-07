package me.bewf.hitspan.util;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import me.bewf.hitspan.config.HitSpanConfig;
import net.minecraft.util.EnumChatFormatting;

public final class NotificationManager {
    
    private NotificationManager() {}
    
    public static void showUpdateNotificationWithConfigTip() {
        if (HitSpanConfig.INSTANCE == null || HitSpanConfig.INSTANCE.hasShownConfigTip) return;
        
        HitSpanConfig.INSTANCE.hasShownConfigTip = true;
        HitSpanConfig.saveConfig();
        
        UChat.chat(EnumChatFormatting.AQUA + "[HitSpan] " + EnumChatFormatting.GRAY + 
                  "You can disable update notifications in the OneConfig menu under " + 
                  EnumChatFormatting.DARK_GRAY + "Debug > Updates");
        UChat.chat("");
    }
}
