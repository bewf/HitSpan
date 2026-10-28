package me.bewf.hitspan.commands;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import me.bewf.hitspan.HitSpan;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.util.UpdateChecker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class HitSpanCommand extends CommandBase {

    @Override
    public String getName() {
        return "hitspan";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/hitspan <command>";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length == 0) {
            UChat.chat(TextFormatting.AQUA + "[HitSpan] " + TextFormatting.GOLD + "Available commands:");
            
            // Create command components with hover info
            TextComponentString disableCmd = new TextComponentString(TextFormatting.GRAY + "disableupdate");
            TextComponentString enableCmd = new TextComponentString(TextFormatting.GRAY + "enableupdate");
            TextComponentString checkCmd = new TextComponentString(TextFormatting.GRAY + "checkupdate");
            
            // Add hover text for checkupdate command
            checkCmd.getStyle().setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new TextComponentString(TextFormatting.LIGHT_PURPLE + "Manually check for updates")
            ));
            
            UChat.chat(TextFormatting.DARK_GRAY + " < " + 
                      disableCmd.getFormattedText() + 
                      TextFormatting.DARK_GRAY + " | " + 
                      enableCmd.getFormattedText() + 
                      TextFormatting.DARK_GRAY + " | " + 
                      checkCmd.getFormattedText() + 
                      TextFormatting.DARK_GRAY + " >");
            return;
        }

        String command = args[0].toLowerCase();
        
        switch (command) {
            case "disableupdate":
                if (!HitSpanConfig.INSTANCE.updateCheckerEnabled) {
                    UChat.chat(TextFormatting.AQUA + "[HitSpan] " + TextFormatting.GRAY + "Update checker is already disabled");
                    return;
                }
                
                HitSpanConfig.INSTANCE.updateCheckerEnabled = false;
                HitSpanConfig.saveConfig();
                UChat.chat(TextFormatting.AQUA + "[HitSpan] " + TextFormatting.RED + "Update checker disabled");
                break;
                
            case "enableupdate":
                if (HitSpanConfig.INSTANCE.updateCheckerEnabled) {
                    UChat.chat(TextFormatting.AQUA + "[HitSpan] " + TextFormatting.GRAY + "Update checker is already enabled");
                    return;
                }
                
                HitSpanConfig.INSTANCE.updateCheckerEnabled = true;
                HitSpanConfig.saveConfig();
                UChat.chat(TextFormatting.AQUA + "[HitSpan] " + TextFormatting.GREEN + "Update checker enabled");
                break;
                
            case "checkupdate":
                UChat.chat(TextFormatting.AQUA + "[HitSpan] " + TextFormatting.GOLD + "Checking for updates...");
                
                // Run update check in separate thread to avoid blocking
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
                        
                        // Add fallback message if no update message was shown after 3 seconds
                        new Thread(() -> {
                            try {
                                Thread.sleep(3000);
                                UChat.chat(TextFormatting.AQUA + "[HitSpan] " + TextFormatting.GREEN + "Up to date");
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                        }).start();
                        
                    } catch (Exception e) {
                        UChat.chat(TextFormatting.AQUA + "[HitSpan] " + TextFormatting.RED + "Failed to check for updates: " + e.getMessage());
                    }
                }).start();
                break;
                
            default:
                UChat.chat(TextFormatting.AQUA + "[HitSpan] " + TextFormatting.GOLD + "Available commands:");
                
                // Create command components with hover info
                TextComponentString disableCmd = new TextComponentString(TextFormatting.GRAY + "disableupdate");
                TextComponentString enableCmd = new TextComponentString(TextFormatting.GRAY + "enableupdate");
                TextComponentString checkCmd = new TextComponentString(TextFormatting.GRAY + "checkupdate");
                
                // Add hover text for checkupdate command
                checkCmd.getStyle().setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new TextComponentString(TextFormatting.LIGHT_PURPLE + "Manually check for updates")
                ));
                
                UChat.chat(TextFormatting.DARK_GRAY + " < " + 
                          disableCmd.getFormattedText() + 
                          TextFormatting.DARK_GRAY + " | " + 
                          enableCmd.getFormattedText() + 
                          TextFormatting.DARK_GRAY + " | " + 
                          checkCmd.getFormattedText() + 
                          TextFormatting.DARK_GRAY + " >");
                break;
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Anyone can use
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }
}
