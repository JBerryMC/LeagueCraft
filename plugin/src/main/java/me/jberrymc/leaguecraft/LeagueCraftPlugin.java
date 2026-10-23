package me.jberrymc.leaguecraft;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * LeagueCraft's server-side helper. The datapack stays the source of truth for champion logic;
 * the plugin takes over the parts a datapack can only fake, starting with input.
 */
public final class LeagueCraftPlugin extends JavaPlugin {
    /** How often champions' hand slot and weapon copy are checked. */
    private static final long HAND_CHECK_TICKS = 5;
    /** HUD labels are re-applied every other check (the datapack resets them when it rebuilds the hotbar). */
    private static final int LABEL_EVERY_N_CHECKS = 2;

    static final String KEY_SETUP_TIP =
            "<gold><bold>LeagueCraft controls</bold></gold> <gray>(League's WASD layout)</gray>\n"
            + "<gray>Left click <white>attack</white> · Right click <white>Q</white> · Shift <white>W</white> · "
            + "<white>E</white>/<white>R</white> · Q <white>summoner 1</white> · F <white>summoner 2</white>\n"
            + "<gray>Minecraft can't tell the server about the E and R keys, so they go through hotbar slots 3 and 4. "
            + "Set them up once in <white>Options › Controls › Key Binds</white>:\n"
            + "<gray> • Inventory → <white>I</white>   • Hotbar Slot 3 → <white>E</white>   • Hotbar Slot 4 → <white>R</white>\n"
            + "<gray>Number keys 1-4 also cast Q/W/E/R. <white>/lc keys</white> shows this again.";

    private volatile InputSettings settings;
    private Datapack datapack;
    private WeaponDisplay weapon;
    private final DevLog dev = new DevLog();
    private AbilityInputListener input;
    private NamespacedKey tipShown;
    private int checks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = InputSettings.from(getConfig(), getLogger());
        datapack = new Datapack(getLogger(), () -> settings.debug());
        weapon = new WeaponDisplay(this);
        tipShown = new NamespacedKey(this, "key_tip_shown");

        input = new AbilityInputListener(datapack, weapon, dev, () -> settings, getLogger());
        getServer().getPluginManager().registerEvents(input, this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                event -> LeagueCraftCommand.register(event.registrar(), this));

        Bukkit.getScheduler().runTaskTimer(this, this::checkPlayers, HAND_CHECK_TICKS, HAND_CHECK_TICKS);

        // The world's datapacks load before plugins enable, but check a tick later to be safe.
        Bukkit.getScheduler().runTask(this, () -> {
            if (!datapack.installed()) {
                getLogger().warning("LeagueCraft datapack not detected (no 'use_ability' objective). "
                        + "Abilities won't cast until the datapack is in the world and /reload has run.");
            }
        });
    }

    @Override
    public void onDisable() {
        // Don't leave weapon copies behind if the plugin is removed mid-game.
        for (Player player : Bukkit.getOnlinePlayers()) weapon.remove(player);
    }

    InputSettings settings() {
        return settings;
    }

    Datapack datapack() {
        return datapack;
    }

    DevLog dev() {
        return dev;
    }

    void reload() {
        reloadConfig();
        settings = InputSettings.from(getConfig(), getLogger());
    }

    void setDebug(boolean debug) {
        getConfig().set("debug", debug);
        saveConfig();
        reload();
    }

    private void checkPlayers() {
        InputSettings config = settings;
        boolean labels = ++checks % LABEL_EVERY_N_CHECKS == 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!input.canCast(player, config)) {
                weapon.remove(player);
                continue;
            }
            weapon.apply(player);
            if (labels) datapack.writeLabels(player, config.hudLabels());
            if (config.firstTimeTip() && !player.getPersistentDataContainer().has(tipShown)) {
                player.getPersistentDataContainer().set(tipShown, PersistentDataType.BYTE, (byte) 1);
                player.sendMessage(MiniMessage.miniMessage().deserialize(KEY_SETUP_TIP));
            }
        }
    }
}
