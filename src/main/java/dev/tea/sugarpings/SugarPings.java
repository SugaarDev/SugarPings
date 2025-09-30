package dev.tea.sugarpings;

import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.util.Tick;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SugarPings extends JavaPlugin implements Listener {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#[a-fA-F0-9]{6}");
    private final TextReplacementConfig.Builder replacementConfigTemplateBuilder = TextReplacementConfig
            .builder()
            .match("%player%");
    private boolean needActionBar;
    private boolean needSound;
    private boolean needTitle;
    private boolean rangedChat;
    private TextComponent actionBarTemplate;
    private Sound sound;
    private float soundVolume;
    private float soundPitch;
    private TextComponent titleTemplate;
    private TextComponent subtitleTemplate;
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
        sound = Registry.SOUNDS.getOrThrow(NamespacedKey.minecraft(soundId));
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

    private @NotNull TextComponent format(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        while (matcher.find()) {
            String color = text.substring(matcher.start() + 1, matcher.end());
            text = text.replace("&" + color, TextColor.fromHexString(color) + "");
            matcher = HEX_PATTERN.matcher(text);
        }
        return LegacyComponentSerializer.legacy('&').deserialize(text);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        String msgLower = ((TextComponent) event.message()).content().toLowerCase();
        Player sender = event.getPlayer();
        Location senderLoc = sender.getLocation();
        org.bukkit.World senderWorld = sender.getWorld();

        Collection<? extends Player> candidates = rangedChat ? senderWorld.getPlayers() : Bukkit.getOnlinePlayers();

        for (Player target : candidates) {
            if (rangedChat && target.getLocation().distanceSquared(senderLoc) >= rangeSquared) continue;

            String targetLowerName = target.getName().toLowerCase();
            if (msgLower.contains(targetLowerName)) {
                if (needActionBar) {
                    Component act = actionBarTemplate.replaceText(
                            replacementConfigTemplateBuilder
                                    .replacement(sender.getName())
                                    .build()
                    );
                    target.sendActionBar(act);
                }
                if (needSound) {
                    target.playSound(target.getLocation(), sound, soundVolume, soundPitch);
                }
                if (needTitle) {
                    Component title = titleTemplate.replaceText(
                            replacementConfigTemplateBuilder
                                    .replacement(sender.getName())
                                    .build()
                    );
                    Component subtitle = subtitleTemplate.replaceText(
                            replacementConfigTemplateBuilder
                                    .replacement(sender.getName())
                                    .build()
                    );

                    target.showTitle(Title.title(
                            title,
                            subtitle,
                            Title.Times.times(Tick.of(titleFadeIn), Tick.of(titleStay), Tick.of(titleFadeOut))
                    ));
                }
            }
        }
    }
}
