package com.yxzhou2025.emanager.ef;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class main extends JavaPlugin implements Listener {

    private File configFile;
    private FileConfiguration config;
    private File logFile;

    @Override
    public void onEnable() {
        // 创建插件数据文件夹
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // 初始化配置文件
        setupConfig();

        // 初始化日志文件
        setupLogFile();

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);

        // 注册命令
        getCommand("emef").setExecutor(new EManagerCommand(this));

        getLogger().info("E-Manager-EF 插件已启用！");
    }

    @Override
    public void onDisable() {
        getLogger().info("E-Manager-EF 插件已禁用！");
    }

    private void setupConfig() {
        configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            // 手动创建默认配置，而不是使用 saveDefaultConfig()
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void createDefaultConfig() {
        try {
            configFile.createNewFile();
            FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);

            // 设置默认配置
            defaultConfig.set("settings.enabled", true);
            defaultConfig.set("settings.broadcast-enabled", true);
            defaultConfig.set("settings.logging-enabled", true);

            defaultConfig.set("blocked-explosions", java.util.Arrays.asList(
                    "TNT", "CREEPER", "FIREBALL", "WITHER_SKULL",
                    "ENDER_CRYSTAL", "MINECART_TNT"
            ));

            defaultConfig.save(configFile);
            getLogger().info("默认配置文件已创建！");
        } catch (IOException e) {
            getLogger().warning("无法创建配置文件: " + e.getMessage());
        }
    }

    private void setupLogFile() {
        File pluginsFolder = new File("plugins");
        File eManagerFolder = new File(pluginsFolder, "E-Manager-EF");
        if (!eManagerFolder.exists()) {
            eManagerFolder.mkdirs();
        }

        logFile = new File(eManagerFolder, "explosion_log.yml");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
                // 初始化日志文件结构
                FileConfiguration logConfig = YamlConfiguration.loadConfiguration(logFile);
                logConfig.set("log-version", "1.0");
                logConfig.set("created-at", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                logConfig.save(logFile);
            } catch (IOException e) {
                getLogger().warning("无法创建日志文件: " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!config.getBoolean("settings.enabled", true)) {
            return;
        }

        // 拦截TNT等爆炸物的爆炸
        if (event.getEntity() instanceof TNTPrimed) {
            event.setCancelled(true);
            handleExplosion(event.getEntity(), "TNT");
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!config.getBoolean("settings.enabled", true)) {
            return;
        }

        // 拦截其他类型的爆炸
        String explosionType = event.getEntityType().toString();
        java.util.List<String> blockedExplosions = config.getStringList("blocked-explosions");

        if (blockedExplosions.contains(explosionType)) {
            event.setCancelled(true);
            handleExplosion(event.getEntity(), explosionType);
        }
    }

    private void handleExplosion(org.bukkit.entity.Entity entity, String explosionType) {
        Location loc = entity.getLocation();
        World world = loc.getWorld();

        // 获取坐标信息
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        String worldName = world.getName();

        // 获取玩家信息（如果是玩家放置的）
        String playerName = "未知";
        if (entity instanceof TNTPrimed) {
            TNTPrimed tnt = (TNTPrimed) entity;
            if (tnt.getSource() instanceof Player) {
                playerName = ((Player) tnt.getSource()).getName();
            }
        }

        // 创建时间戳
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // 向全服玩家发送提示消息（如果启用广播）
        if (config.getBoolean("settings.broadcast-enabled", true)) {
            String broadcastMessage = ChatColor.RED + "[爆炸拦截] " +
                    ChatColor.YELLOW + "检测到 " + explosionType + " 爆炸已被拦截！" +
                    ChatColor.WHITE + " 位置: " + worldName + " (" + x + ", " + y + ", " + z + ")" +
                    (playerName.equals("未知") ? "" : " 玩家: " + playerName);

            Bukkit.broadcastMessage(broadcastMessage);
        }

        // 记录到日志文件（如果启用日志）
        if (config.getBoolean("settings.logging-enabled", true)) {
            logExplosionEvent(timestamp, explosionType, worldName, x, y, z, playerName);
        }

        // 在控制台也输出信息
        getLogger().info("拦截爆炸: " + explosionType + " 位置: " + worldName + " (" + x + ", " + y + ", " + z + ") 玩家: " + playerName);
    }

    private void logExplosionEvent(String timestamp, String explosionType, String world, int x, int y, int z, String player) {
        FileConfiguration logConfig = YamlConfiguration.loadConfiguration(logFile);

        // 创建唯一的事件ID
        String eventId = "event_" + System.currentTimeMillis();

        // 保存事件信息
        logConfig.set("events." + eventId + ".timestamp", timestamp);
        logConfig.set("events." + eventId + ".type", explosionType);
        logConfig.set("events." + eventId + ".world", world);
        logConfig.set("events." + eventId + ".x", x);
        logConfig.set("events." + eventId + ".y", y);
        logConfig.set("events." + eventId + ".z", z);
        logConfig.set("events." + eventId + ".player", player);

        try {
            logConfig.save(logFile);
        } catch (IOException e) {
            getLogger().warning("无法保存日志文件: " + e.getMessage());
        }
    }

    public FileConfiguration getLogConfig() {
        return YamlConfiguration.loadConfiguration(logFile);
    }

    public File getLogFile() {
        return logFile;
    }

    public void reloadPluginConfig() {
        setupConfig();
    }
}