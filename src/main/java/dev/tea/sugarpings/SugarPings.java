package dev.tea.sugarpings;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SugarPings extends JavaPlugin implements Listener {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");

    private boolean needActionBar;
    private boolean needSound;
    private boolean needTitle;
    private boolean rangedChat;

    private String actionBarTemplate;
    private Sound sound;
    private float soundVolume;
    private float soundPitch;
    private String titleTemplate;
    private String subtitleTemplate;
    private int titleFadeIn;
    private int titleStay;
    private int titleFadeOut;
    private double rangeSquared;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        FileConfiguration config = getConfig();
        needActionBar = config.getBoolean("NeedActionBar");
        needSound = config.getBoolean("NeedSound");
        needTitle = config.getBoolean("NeedTitle");
        rangedChat = config.getBoolean("RangedChat");

        actionBarTemplate = format(config.getString("ActionBar", ""));
        String soundId = config.getString("Sound.ID", "ENTITY_ARROW_HIT_PLAYER");
        sound = Sound.valueOf(soundId);
        soundVolume = (float) config.getDouble("Sound.Volume", 1.0);
        soundPitch = (float) config.getDouble("Sound.Pitch", 1.0);
        titleTemplate = format(config.getString("Title.Title", "&f"));
        subtitleTemplate = format(config.getString("Title.SubTitle", "&f"));
        titleFadeIn = config.getInt("Title.FadeIn", 20);
        titleStay = config.getInt("Title.Stay", 20);
        titleFadeOut = config.getInt("Title.FadeOut", 20);

        int range = config.getInt("Range", 100);
        rangeSquared = range * range;

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    private String format(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        while (matcher.find()) {
            String color = text.substring(matcher.start() + 1, matcher.end());
            text = text.replace("&" + color, ChatColor.of(color) + "");
            matcher = HEX_PATTERN.matcher(text);
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        String msgLower = event.getMessage().toLowerCase();
        Player sender = event.getPlayer();
        Location senderLoc = sender.getLocation();
        org.bukkit.World senderWorld = sender.getWorld();

        Iterable<Player> candidates = rangedChat ? senderWorld.getPlayers() : Bukkit.getOnlinePlayers();

        for (Player target : candidates) {
            if (rangedChat && target.getLocation().distanceSquared(senderLoc) >= rangeSquared) continue;

            String targetLowerName = target.getName().toLowerCase();
            if (msgLower.contains(targetLowerName)) {
                if (needActionBar) {
                    String act = actionBarTemplate.replace("%player%", sender.getName());
                    target.sendActionBar(act);
                }
                if (needSound) {
                    target.playSound(target.getLocation(), sound, soundVolume, soundPitch);
                }
                if (needTitle) {
                    String title = titleTemplate.replace("%player%", sender.getName());
                    String subtitle = subtitleTemplate.replace("%player%", sender.getName());
                    target.sendTitle(title, subtitle, titleFadeIn, titleStay, titleFadeOut);
                }
            }
        }
    }
}
