package me.jberrymc.leaguecraft;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * LeagueCraft's server-side helper. The datapack stays the source of truth for champion logic;
 * the plugin takes over the parts a datapack can only fake: input, the third-person view and the
 * hitscan basic attack.
 */
public final class LeagueCraftPlugin extends JavaPlugin {
    /** How often champions' hand slot and weapon copy are checked. */
    private static final long HAND_CHECK_TICKS = 5;

    static final String KEY_SETUP_TIP =
            "<gold><bold>LeagueCraft controls</bold></gold>\n"
            + "<gray>Press <white>F5</white> for the over-the-shoulder view. Aim with the crosshair; hold <white>right click</white> to zoom.\n"
            + "<gray>Left click <white>attack</white> · <white>1 2 3 4</white> abilities Q W E R · "
            + "Q <white>summoner 1</white> · F <white>summoner 2</white> · Ctrl <white>reload</white>\n"
            + "<gray>Abilities cast on the ground: press once to aim, again or <white>left click</white> to cast, "
            + "<white>Shift</white> to cancel.\n"
            + "<gray>Abilities follow your <white>Hotbar Slot 1-4</white> binds, so you can move them to other keys in "
            + "<white>Options › Controls › Key Binds</white>; the HUD shows your keys. <white>/lc keys</white> shows this again.";

    private volatile InputSettings settings;
    private volatile CombatSettings combat;
    private Datapack datapack;
    private WeaponDisplay weapon;
    private ThirdPerson thirdPerson;
    private MovementLock movementLock;
    private Ammo ammo;
    private ChampionModel model;
    private RigTest rigTest;
    private final DevLog dev = new DevLog();
    private AbilityInputListener input;
    private NamespacedKey tipShown;
    /** Players who were champions at the last check, to spot someone becoming one. */
    private final Set<UUID> champions = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        migrateConfig();
        settings = InputSettings.from(getConfig(), getLogger());
        combat = CombatSettings.from(getConfig(), getLogger());
        datapack = new Datapack(getLogger(), () -> settings.debug());
        weapon = new WeaponDisplay(this);
        thirdPerson = new ThirdPerson(datapack, dev, () -> combat, getLogger());
        movementLock = new MovementLock(() -> combat, thirdPerson::zoomView, getLogger());
        ammo = new Ammo(datapack, dev, () -> combat);
        tipShown = new NamespacedKey(this, "key_tip_shown");

        input = new AbilityInputListener(datapack, weapon, dev, () -> settings, () -> combat,
                new AimPoint(() -> combat, getLogger()), getLogger());
        getServer().getPluginManager().registerEvents(input, this);
        getServer().getPluginManager().registerEvents(
                new BasicAttackListener(input, datapack, ammo, dev, () -> settings, () -> combat), this);
        getServer().getPluginManager().registerEvents(ammo, this);
        getServer().getPluginManager().registerEvents(new Motion(datapack), this);
        getServer().getPluginManager().registerEvents(new MenuListener(datapack), this);
        Bukkit.getScheduler().runTaskTimer(this, ammo::tick, 1, 1);
        getServer().getPluginManager().registerEvents(thirdPerson, this);
        getServer().getPluginManager().registerEvents(movementLock, this);
        Bukkit.getScheduler().runTaskTimer(this, thirdPerson::tick, 1, 1);

        Indicators indicators = new Indicators(datapack, () -> combat, input::aiming);
        Bukkit.getScheduler().runTaskTimer(this, indicators::tick, 2, 2);
        try {
            rigTest = new RigTest(Rig.load(this, "aatrox"), dev);
            getServer().getPluginManager().registerEvents(rigTest, this);
            Bukkit.getScheduler().runTaskTimer(this, rigTest::tick, 1, 1);
        } catch (IOException | RuntimeException e) {
            getLogger().warning("Couldn't load the aatrox rig for /lc rigtest: " + e.getMessage());
        }

        model = new ChampionModel(player -> rigTest != null && rigTest.has(player));
        getServer().getPluginManager().registerEvents(model, this);
        Bukkit.getScheduler().runTaskTimer(this, model::tick, 1, 1);

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
            ammo.remove(player);
        }
        if (rigTest != null) rigTest.removeAll();
        model.removeAll();
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
        // 0.7.0: the datapack moved into one namespace, so _champion:x is lc:champion/x now.
        for (String path : List.of("cast-function", "summoner-1-function", "summoner-2-function")) {
            String function = config.getString(path, "");
            if (function.startsWith("_champion:")) {
                config.set(path, "lc:champion/" + function.substring("_champion:".length()));
                changes.add(path + " " + function + " -> " + config.getString(path));
            }
        }
        // 0.7.0: range and attack speed are champion stats in the datapack's stats chart now.
        for (String removed : List.of("basic-attack.melee-range", "basic-attack.ranged-range", "basic-attack.cooldown-ticks")) {
            if (config.isSet(removed)) {
                config.set(removed, null);
                changes.add("removed " + removed + " (now a champion stat in the datapack's settings.mcb)");
            }
        }
        // 0.7.3: indicators no longer follow the zoom; only the aimed ability shows, or every ready one.
        String indicators = config.getString("abilities.indicators", "aimed");
        if (indicators.equalsIgnoreCase("zoom") || indicators.equalsIgnoreCase("off")) {
            config.set("abilities.indicators", "aimed");
            changes.add("abilities.indicators " + indicators + " -> aimed");
        }
        // 0.7.2: zoom.slowdown is per champion (melee / ranged / champions), not one number.
        if (config.isSet("zoom.slowdown") && !config.isConfigurationSection("zoom.slowdown")) {
            double old = config.getDouble("zoom.slowdown");
            config.set("zoom.slowdown", null);
            config.set("zoom.slowdown.melee", old > 0 ? old : 0.2);
            config.set("zoom.slowdown.ranged", old > 0 ? old : 0.5);
            config.set("zoom.slowdown.champions", new java.util.HashMap<String, Double>());
            changes.add("zoom.slowdown " + old + " -> melee " + config.getDouble("zoom.slowdown.melee")
                    + ", ranged " + config.getDouble("zoom.slowdown.ranged"));
        }
        // 0.7.1: zoom narrows the view without slowing (zoom.view.*); the old zoom.melee/ranged/champions
        // were slowdowns, which narrowed the view by half as much. (Before 0.7.0 they were camera
        // distances of 1 or more, which are dropped.)
        for (String key : List.of("melee", "ranged")) {
            String path = "zoom." + key;
            if (!config.isSet(path)) continue;
            double old = config.getDouble(path);
            config.set(path, null);
            if (old > 0 && old < 1) {
                config.set("zoom.view." + key, Math.min(CombatSettings.MAX_ZOOM, old / 2));
                changes.add(path + " " + old + " -> zoom.view." + key + " " + config.getDouble("zoom.view." + key));
            } else {
                changes.add("removed " + path);
            }
        }
        ConfigurationSection zoomChampions = config.getConfigurationSection("zoom.champions");
        if (zoomChampions != null) {
            for (String champion : zoomChampions.getKeys(false)) {
                double old = zoomChampions.getDouble(champion, -1);
                if (old > 0 && old < 1) config.set("zoom.view.champions." + champion, Math.min(CombatSettings.MAX_ZOOM, old / 2));
            }
            config.set("zoom.champions", null);
            changes.add("moved zoom.champions to zoom.view.champions");
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
        champions.removeIf(id -> Bukkit.getPlayer(id) == null);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!input.canCast(player, config)) {
                champions.remove(player.getUniqueId());
                weapon.remove(player);
                thirdPerson.remove(player);
                movementLock.remove(player);
                ammo.remove(player);
                model.remove(player);
                continue;
            }
            // The datapack sets the default labels itself; custom ones are written when someone
            // becomes a champion and after each cast (its reload puts the defaults back).
            if (champions.add(player.getUniqueId())) datapack.writeLabels(player, config.hudLabels());
            weapon.apply(player);
            thirdPerson.apply(player, combatConfig);
            movementLock.apply(player, combatConfig);
            ammo.apply(player);
            model.apply(player);
            if (config.firstTimeTip() && !player.getPersistentDataContainer().has(tipShown)) {
                player.getPersistentDataContainer().set(tipShown, PersistentDataType.BYTE, (byte) 1);
                player.sendMessage(MiniMessage.miniMessage().deserialize(KEY_SETUP_TIP));
            }
        }
    }
}
