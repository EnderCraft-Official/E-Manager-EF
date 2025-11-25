package com.yxzhou2025.emanager.ef;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class EManagerCommand implements CommandExecutor {

    private final main plugin;

    public EManagerCommand(main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (sender.hasPermission("emanager.emef.reload")) {
                    plugin.reloadConfig();
                    sender.sendMessage(ChatColor.GREEN + "配置文件已重新加载！");
                } else {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                }
                break;

            case "logs":
                if (sender.hasPermission("emanager.emef.logs")) {
                    showLogs(sender, args);
                } else {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                }
                break;

            case "clearlogs":
                if (sender.hasPermission("emanager.emef.clearlogs")) {
                    clearLogs(sender);
                } else {
                    sender.sendMessage(ChatColor.RED + "你没有权限执行此命令！");
                }
                break;

            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== E-Manager-EF 帮助 ===");
        sender.sendMessage(ChatColor.YELLOW + "/emef reload - 重新加载配置");
        sender.sendMessage(ChatColor.YELLOW + "/emef logs [数量] - 查看爆炸日志");
        sender.sendMessage(ChatColor.YELLOW + "/emef clearlogs - 清空日志");
    }

    private void showLogs(CommandSender sender, String[] args) {
        FileConfiguration logConfig = plugin.getLogConfig();

        if (!logConfig.contains("events")) {
            sender.sendMessage(ChatColor.YELLOW + "暂无爆炸拦截记录。");
            return;
        }

        Set<String> events = logConfig.getConfigurationSection("events").getKeys(false);
        int totalEvents = events.size();
        int pageSize = 10;

        if (args.length > 1) {
            try {
                pageSize = Integer.parseInt(args[1]);
                pageSize = Math.min(Math.max(pageSize, 1), 50); // 限制在1-50之间
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "无效的数量！");
                return;
            }
        }

        sender.sendMessage(ChatColor.GOLD + "=== 爆炸拦截记录 (共 " + totalEvents + " 条) ===");

        events.stream()
                .sorted((e1, e2) -> Long.compare(
                        Long.parseLong(e2.replace("event_", "")),
                        Long.parseLong(e1.replace("event_", ""))
                ))
                .limit(pageSize)
                .forEach(eventId -> {
                    String timestamp = logConfig.getString("events." + eventId + ".timestamp");
                    String type = logConfig.getString("events." + eventId + ".type");
                    String world = logConfig.getString("events." + eventId + ".world");
                    int x = logConfig.getInt("events." + eventId + ".x");
                    int y = logConfig.getInt("events." + eventId + ".y");
                    int z = logConfig.getInt("events." + eventId + ".z");
                    String player = logConfig.getString("events." + eventId + ".player");

                    String message = ChatColor.YELLOW + "[" + timestamp + "] " +
                            ChatColor.WHITE + type + " - " +
                            world + " (" + x + ", " + y + ", " + z + ")" +
                            (player.equals("未知") ? "" : " - 玩家: " + player);

                    sender.sendMessage(message);
                });
    }

    private void clearLogs(CommandSender sender) {
        FileConfiguration logConfig = plugin.getLogConfig();
        logConfig.set("events", null);

        try {
            logConfig.save(plugin.getLogFile());
            sender.sendMessage(ChatColor.GREEN + "日志已清空！");
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "清空日志时出现错误: " + e.getMessage());
        }
    }
}