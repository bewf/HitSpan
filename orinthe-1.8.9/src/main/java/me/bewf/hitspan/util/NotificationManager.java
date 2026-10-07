package me.bewf.hitspan.util;

import me.bewf.hitspan.config.HitSpanConfig;

public final class NotificationManager {

    private NotificationManager() {}

    public static void showUpdateNotificationWithConfigTip() {
        if (HitSpanConfig.INSTANCE == null || HitSpanConfig.INSTANCE.hasShownConfigTip) return;

        HitSpanConfig.INSTANCE.hasShownConfigTip = true;
        HitSpanConfig.saveConfig();

        Chat.send("\u00A7b[HitSpan] \u00A77You can disable update notifications in the OneConfig menu under \u00A78Debug > Updates");
        Chat.send("");
    }
}
