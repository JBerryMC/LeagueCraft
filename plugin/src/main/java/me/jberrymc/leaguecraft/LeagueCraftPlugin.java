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
 * the plugin takes over the parts a datapack can only fake: input, the third-person view and the
 * hitscan basic attack.
 */
public final class LeagueCraftPlugin extends JavaPlugin {
    /** How often champions' hand slot and weapon copy are checked. */
    private static final long HAND_CHECK_TICKS = 5;
    /** HUD labels are re-applied every other check (the datapack resets them when it rebuilds the hotbar). */
    private static final int LABEL_EVERY_N_CHECKS = 2;

    static final String KEY_SETUP_TIP =
            "<gold><bold>LeagueCraft controls</bold></gold>\n"
            + "<gray>Press <white>F5</white> for the over-the-shoulder view. Aim with the crosshair.\n"
            + "<gray>Left click <white>attack</white> · <white>1 2 3 4</white> abilities Q W E R · "
            + "Q <white>summoner 1</white> · F <white>summoner 2</white>\n"
            + "<gray>Abilities follow your <white>Hotbar Slot 1-4</white> binds, so you can move them to other keys in "
            + "<white>Options › Controls › Key Binds</white>; the HUD shows your keys. <white>/lc keys</white> shows this again.";

    private volatile InputSettings settings;
    private volatile CombatSettings combat;
    private Datapack datapack;
    private WeaponDisplay weapon;
    private ThirdPerson thirdPerson;
    private final DevLog dev = new DevLog();
    private AbilityInputListener input;
    private NamespacedKey tipShown;
    private int checks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = InputSettings.from(getConfig(), getLogger());
        combat = CombatSettings.from(getConfig(), getLogger());
        datapack = new Datapack(getLogger(), () -> settings.debug());
        weapon = new WeaponDisplay(this);
        thirdPerson = new ThirdPerson(datapack, getLogger());
        tipShown = new NamespacedKey(this, "key_tip_shown");

        input = new AbilityInputListener(datapack, weapon, dev, () -> settings, getLogger());
        getServer().getPluginManager().registerEvents(input, this);
        getServer().getPluginManager().registerEvents(
                new BasicAttackListener(input, dev, () -> settings, () -> combat), this);
        getServer().getPluginManager().registerEvents(thirdPerson, this);

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
        // Don't leave weapon copies or a changed camera behind if the plugin is removed mid-game.
        for (Player player : Bukkit.getOnlinePlayers()) {
            weapon.remove(player);
            thirdPerson.remove(player);
        }
    }

    InputSettings settings() {
        return settings;
    }

    CombatSettings combat() {
        return combat;
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
        combat = CombatSettings.from(getConfig(), getLogger());
    }

    void setDebug(boolean debug) {
        getConfig().set("debug", debug);
        saveConfig();
        reload();
    }

    private void checkPlayers() {
        InputSettings config = settings;
        CombatSettings combatConfig = combat;
        boolean labels = ++checks % LABEL_EVERY_N_CHECKS == 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!input.canCast(player, config)) {
                weapon.remove(player);
                thirdPerson.remove(player);
                continue;
            }
            weapon.apply(player);
            thirdPerson.apply(player, combatConfig, labels);
            if (labels) datapack.writeLabels(player, config.hudLabels());
            if (config.firstTimeTip() && !player.getPersistentDataContainer().has(tipShown)) {
                player.getPersistentDataContainer().set(tipShown, PersistentDataType.BYTE, (byte) 1);
                player.sendMessage(MiniMessage.miniMessage().deserialize(KEY_SETUP_TIP));
            }
        }
    }
}
