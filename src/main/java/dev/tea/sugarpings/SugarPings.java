package dev.tea.sugarpings;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SugarPings extends JavaPlugin implements Listener {

    private FileConfiguration config;
    private boolean needSound;
    private boolean needAct;
    private boolean needTitle;
    private boolean rangedChat;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        config = getConfig();
        needSound = config.getBoolean("NeedSound");
        needAct = config.getBoolean("NeedActionBar");
        needTitle = config.getBoolean("NeedTitle");
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
        return player1.getLocation().distance(player2.getLocation()) >= config.getInt("Range");
    }

    @EventHandler
    public void onChat (io.papermc.paper.event.player.AsyncChatEvent event) {
        Component msg = event.message();
        for (Player player1 : Bukkit.getOnlinePlayers()) {

            if (rangedChat && event.getPlayer().getWorld() != player1.getWorld()) continue;
            if (rangedChat && !rangedChatCheck(event.getPlayer(), player1)) continue;

            if (msg.contains(Component.text(player1.getName().toLowerCase()))) continue; {
                Audience audience = Audience.audience(player1);
                String act = format(config.getString("ActionBar"));
                if (act.contains("%player%")) act = act.replace("%player%", event.getPlayer().getName());
                if (needAct) {
                    audience.sendActionBar(
                            Component.text(act)
                    );
                }
                if (needSound) { player1.playSound(player1.getLocation(), Sound.valueOf(config.getString("Sound.ID")), config.getInt("Sound.Volume"), config.getInt("Sound.Pitch")); }
                if (needTitle) {
                    String title = format(config.getString("Title.Title", "&f"));
                    String subtitle = format(config.getString("Title.SubTitle", "&f"));
                    int fadein = config.getInt("Title.FadeIn", 20);
                    int stay = config.getInt("Title.Stay", 20);
                    int fadeout = config.getInt("Title.FadeOut", 20);
                    if (title.contains("%player")) title = title.replace("%player%", event.getPlayer().getName());
                    if (subtitle.contains("%player")) subtitle = subtitle.replace("%player%", event.getPlayer().getName());
                    audience.showTitle(Title.title(Component.text(title), Component.text(subtitle), Title.Times.of(
                            Duration.ofSeconds((long) fadein/20), Duration.ofSeconds((long) stay/20), Duration.ofSeconds((long) fadeout/20)
                    )));
                }
            }
        }
    }

}
