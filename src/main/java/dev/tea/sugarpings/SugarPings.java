package dev.tea.sugarpings;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SugarPings extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private boolean needSound;
    private boolean needAct;
    private boolean rangedChat;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        config = getConfig();
        needSound = config.getBoolean("NeedActionBar");
        needAct = config.getBoolean("NeedSound");
        rangedChat = config.getBoolean("RangedChat");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private String format (String text) {
        Pattern pattern = Pattern.compile("&#[a-fA-F0-9]{6}");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String color = text.substring(matcher.start() + 1, matcher.end());
            text = text.replace("&" + color, ChatColor.of(color) + "");
            matcher = pattern.matcher(text);
        }

        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private boolean rangedChatCheck (Player player1, Player player2) {
        Location loc1 = player1.getLocation();
        Location loc2 = player2.getLocation();
        if (loc1.distance(loc2) >= config.getInt("Range")) return false;
        return true;
    }

    @EventHandler
    public void onChat (PlayerChatEvent event) {
        String msg = event.getMessage().toLowerCase();
        for (Player player1 : Bukkit.getOnlinePlayers()) {

            if (rangedChat && event.getPlayer().getWorld() != player1.getWorld()) continue;
            if (rangedChat && !rangedChatCheck(event.getPlayer(), player1)) continue;

            if (msg.contains(player1.getName().toLowerCase())) {
                String act = format(config.getString("ActionBar"));
                if (act.contains("%player%")) act = act.replace("%player%", event.getPlayer().getName());
                if (needAct) player1.sendActionBar(act);
                if (needSound) { player1.playSound(player1.getLocation(), Sound.valueOf(config.getString("Sound.ID")), config.getInt("Sound.Volume"), config.getInt("Sound.Pitch")); }
            }
        }
    }

}
