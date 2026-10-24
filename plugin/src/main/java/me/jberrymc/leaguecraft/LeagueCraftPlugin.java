package me.jberrymc.leaguecraft;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            + "<gray>Press <white>F5</white> for the over-the-shoulder view. Aim with the crosshair; hold <white>right click</white> to zoom.\n"
            + "<gray>Left click <white>attack</white> · <white>1 2 3 4</white> abilities Q W E R · "
            + "Q <white>summoner 1</white> · F <white>summoner 2</white>\n"
            + "<gray>Abilities follow your <white>Hotbar Slot 1-4</white> binds, so you can move them to other keys in "
            + "<white>Options › Controls › Key Binds</white>; the HUD shows your keys. <white>/lc keys</white> shows this again.";

    private volatile InputSettings settings;
    private volatile CombatSettings combat;
    private Datapack datapack;
    private WeaponDisplay weapon;
    private ThirdPerson thirdPerson;
    private MovementLock movementLock;
    private RigTest rigTest;
    private final DevLog dev = new DevLog();
    private AbilityInputListener input;
    private NamespacedKey tipShown;
    private int checks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateConfig();
        settings = InputSettings.from(getConfig(), getLogger());
        combat = CombatSettings.from(getConfig(), getLogger());
        datapack = new Datapack(getLogger(), () -> settings.debug());
        weapon = new WeaponDisplay(this);
        thirdPerson = new ThirdPerson(datapack, dev, () -> combat, getLogger());
        movementLock = new MovementLock(() -> combat, getLogger());
        tipShown = new NamespacedKey(this, "key_tip_shown");

        input = new AbilityInputListener(datapack, weapon, dev, () -> settings, getLogger());
        getServer().getPluginManager().registerEvents(input, this);
        getServer().getPluginManager().registerEvents(
                new BasicAttackListener(input, dev, () -> settings, () -> combat), this);
        getServer().getPluginManager().registerEvents(thirdPerson, this);
        getServer().getPluginManager().registerEvents(movementLock, this);
        Bukkit.getScheduler().runTaskTimer(this, thirdPerson::tick, 1, 1);
        try {
            rigTest = new RigTest(Rig.load(this, "aatrox"), dev);
            getServer().getPluginManager().registerEvents(rigTest, this);
            Bukkit.getScheduler().runTaskTimer(this, rigTest::tick, 1, 1);
        } catch (IOException | RuntimeException e) {
            getLogger().warning("Couldn't load the aatrox rig for /lc rigtest: " + e.getMessage());
        }

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
            movementLock.remove(player);
        }
        if (rigTest != null) rigTest.removeAll();
    }

    InputSettings settings() {
        return settings;
    }

    CombatSettings combat() {
        return combat;
    }

    /** Null if the test rig failed to load. */
    RigTest rigTest() {
        return rigTest;
    }

    Datapack datapack() {
        return datapack;
    }

    DevLog dev() {
        return dev;
    }

    void reload() {
        reloadConfig();
        migrateConfig();
        settings = InputSettings.from(getConfig(), getLogger());
        combat = CombatSettings.from(getConfig(), getLogger());
    }

    /**
     * Paper only writes config.yml when there isn't one, so a server that updated keeps old
     * settings. Rewrites the ones later versions dropped and saves the file.
     */
    private void migrateConfig() {
        FileConfiguration config = getConfig();
        List<String> changes = new ArrayList<>();
        // 0.5.0: right click and shift no longer cast; abilities are on hotbar keys 1-4.
        for (String removed : List.of("keys.right-click", "keys.sneak", "basic-attack.hitscan")) {
            if (config.isSet(removed)) {
                config.set(removed, null);
                changes.add("removed " + removed);
            }
        }
        // The old default labels for right click and shift.
        if ("M2".equals(config.getString("hud-labels.q"))) {
            config.set("hud-labels.q", "key.hotbar.1");
            changes.add("hud-labels.q M2 -> key.hotbar.1");
        }
        if ("SH".equals(config.getString("hud-labels.w"))) {
            config.set("hud-labels.w", "key.hotbar.2");
            changes.add("hud-labels.w SH -> key.hotbar.2");
        }
        if (changes.isEmpty()) return;
        saveConfig();
        getLogger().info("Updated config.yml from an older version: " + String.join(", ", changes));
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
                movementLock.remove(player);
                continue;
            }
            weapon.apply(player);
            thirdPerson.apply(player, combatConfig, labels);
            movementLock.apply(player, combatConfig);
            if (labels) datapack.writeLabels(player, config.hudLabels());
            if (config.firstTimeTip() && !player.getPersistentDataContainer().has(tipShown)) {
                player.getPersistentDataContainer().set(tipShown, PersistentDataType.BYTE, (byte) 1);
                player.sendMessage(MiniMessage.miniMessage().deserialize(KEY_SETUP_TIP));
            }
        }
    }
}
