package me.bewf.hitspan.commands;

import me.bewf.hitspan.HitSpan;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.util.Chat;
import me.bewf.hitspan.util.UpdateChecker;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;

public final class HitSpanCommand {

    private HitSpanCommand() {}

    public static void register() {
        CommandManager.INSTANCE.register(
                CommandManager.literal("hitspan")
                        .executes(ctx -> {
                            printHelp();
                            return 1;
                        })
                        .then(CommandManager.literal("disableupdate").executes(ctx -> {
                            disableUpdate();
                            return 1;
                        }))
                        .then(CommandManager.literal("enableupdate").executes(ctx -> {
                            enableUpdate();
                            return 1;
                        }))
                        .then(CommandManager.literal("checkupdate").executes(ctx -> {
                            checkUpdate();
                            return 1;
                        }))
        );
    }

    private static void printHelp() {
        Chat.send("\u00A7b[HitSpan] \u00A76Available commands:");
        Chat.send("\u00A78 < \u00A77disableupdate\u00A78 | \u00A77enableupdate\u00A78 | \u00A77checkupdate\u00A78 >");
    }

    private static void disableUpdate() {
        if (!HitSpanConfig.INSTANCE.updateCheckerEnabled) {
            Chat.send("\u00A7b[HitSpan] \u00A77Update checker is already disabled");
            return;
        }

        HitSpanConfig.INSTANCE.updateCheckerEnabled = false;
        HitSpanConfig.saveConfig();
        Chat.send("\u00A7b[HitSpan] \u00A7cUpdate checker disabled");
    }

    private static void enableUpdate() {
        if (HitSpanConfig.INSTANCE.updateCheckerEnabled) {
            Chat.send("\u00A7b[HitSpan] \u00A77Update checker is already enabled");
            return;
        }

        HitSpanConfig.INSTANCE.updateCheckerEnabled = true;
        HitSpanConfig.saveConfig();
        Chat.send("\u00A7b[HitSpan] \u00A7aUpdate checker enabled");
    }

    private static void checkUpdate() {
        Chat.send("\u00A7b[HitSpan] \u00A76Checking for updates...");

        new Thread(() -> {
            try {
                UpdateChecker.checkOnce(
                        "dDmpgD3L",
                        "hitspan",
                        "HitSpan",
                        HitSpan.VERSION,
                        HitSpan.MC_VERSION,
                        HitSpan.LOADER
                );

                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        Chat.send("\u00A7b[HitSpan] \u00A7aUp to date");
                    } catch (InterruptedException ignored) {
                    }
                }).start();

            } catch (Exception e) {
                Chat.send("\u00A7b[HitSpan] \u00A7cFailed to check for updates: " + e.getMessage());
            }
        }).start();
    }
}
