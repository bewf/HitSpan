package me.bewf.hitspan.commands;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import me.bewf.hitspan.HitSpan;
import me.bewf.hitspan.config.HitSpanConfig;
import me.bewf.hitspan.util.UpdateChecker;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;

public class HitSpanCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "hitspan";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/hitspan <command>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            UChat.chat(EnumChatFormatting.AQUA + "[HitSpan] " + EnumChatFormatting.GOLD + "Available commands:");
            
            // Create command components with hover info
            ChatComponentText disableCmd = new ChatComponentText(EnumChatFormatting.GRAY + "disableupdate");
            ChatComponentText enableCmd = new ChatComponentText(EnumChatFormatting.GRAY + "enableupdate");
            ChatComponentText checkCmd = new ChatComponentText(EnumChatFormatting.GRAY + "checkupdate");
            
            // Add hover text for checkupdate command
            checkCmd.getChatStyle().setChatHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "Manually check for updates")
            ));
            
            UChat.chat(EnumChatFormatting.DARK_GRAY + " < " + 
                      disableCmd.getFormattedText() + 
                      EnumChatFormatting.DARK_GRAY + " | " + 
                      enableCmd.getFormattedText() + 
                      EnumChatFormatting.DARK_GRAY + " | " + 
                      checkCmd.getFormattedText() + 
                      EnumChatFormatting.DARK_GRAY + " >");
            return;
        }

        String command = args[0].toLowerCase();
        
        switch (command) {
            case "disableupdate":
                if (!HitSpanConfig.INSTANCE.updateCheckerEnabled) {
                    UChat.chat(EnumChatFormatting.AQUA + "[HitSpan] " + EnumChatFormatting.GRAY + "Update checker is already disabled");
                    return;
                }
                
                HitSpanConfig.INSTANCE.updateCheckerEnabled = false;
                HitSpanConfig.saveConfig();
                UChat.chat(EnumChatFormatting.AQUA + "[HitSpan] " + EnumChatFormatting.RED + "Update checker disabled");
                break;
                
            case "enableupdate":
                if (HitSpanConfig.INSTANCE.updateCheckerEnabled) {
                    UChat.chat(EnumChatFormatting.AQUA + "[HitSpan] " + EnumChatFormatting.GRAY + "Update checker is already enabled");
                    return;
                }
                
                HitSpanConfig.INSTANCE.updateCheckerEnabled = true;
                HitSpanConfig.saveConfig();
                UChat.chat(EnumChatFormatting.AQUA + "[HitSpan] " + EnumChatFormatting.GREEN + "Update checker enabled");
                break;
                
            case "checkupdate":
                UChat.chat(EnumChatFormatting.AQUA + "[HitSpan] " + EnumChatFormatting.GOLD + "Checking for updates...");
                
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
                                UChat.chat(EnumChatFormatting.AQUA + "[HitSpan] " + EnumChatFormatting.GREEN + "Up to date");
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                        }).start();
                        
                    } catch (Exception e) {
                        UChat.chat(EnumChatFormatting.AQUA + "[HitSpan] " + EnumChatFormatting.RED + "Failed to check for updates: " + e.getMessage());
                    }
                }).start();
                break;
                
            default:
                UChat.chat(EnumChatFormatting.AQUA + "[HitSpan] " + EnumChatFormatting.GOLD + "Available commands:");
                
                // Create command components with hover info
                ChatComponentText disableCmd = new ChatComponentText(EnumChatFormatting.GRAY + "disableupdate");
                ChatComponentText enableCmd = new ChatComponentText(EnumChatFormatting.GRAY + "enableupdate");
                ChatComponentText checkCmd = new ChatComponentText(EnumChatFormatting.GRAY + "checkupdate");
                
                // Add hover text for checkupdate command
                checkCmd.getChatStyle().setChatHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ChatComponentText(EnumChatFormatting.LIGHT_PURPLE + "Manually check for updates")
                ));
                
                UChat.chat(EnumChatFormatting.DARK_GRAY + " < " + 
                          disableCmd.getFormattedText() + 
                          EnumChatFormatting.DARK_GRAY + " | " + 
                          enableCmd.getFormattedText() + 
                          EnumChatFormatting.DARK_GRAY + " | " + 
                          checkCmd.getFormattedText() + 
                          EnumChatFormatting.DARK_GRAY + " >");
                break;
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // Anyone can use
    }
}
