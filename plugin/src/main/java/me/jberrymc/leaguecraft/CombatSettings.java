package me.jberrymc.leaguecraft;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** The third-person:, zoom:, movement: and basic-attack: sections of config.yml, read once per load/reload. */
record CombatSettings(
        boolean thirdPerson,
        double cameraDistance,
        double shoulder,
        double height,
        boolean crosshair,
        boolean zoom,
        double zoomMelee,
        double zoomRanged,
        Map<String, Double> zoomChampions,
        boolean disableSprint,
        boolean disableSneak,
        double meleeAngle,
        ReloadKey reloadKey) {

    /** The key that reloads early (a reload also starts by itself when the ammo runs out). */
    enum ReloadKey { SPRINT, SNEAK, NONE }

    /** How much slower a zoomed champion walks (0.3 = 30%), and why (for dev mode). */
    record Zoom(double slow, String source) {}

    static CombatSettings from(FileConfiguration config, Logger log) {
        // zoom.champions: <champion tag> -> slowdown
        Map<String, Double> champions = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("zoom.champions");
        if (section != null) {
            for (String champion : section.getKeys(false)) {
                double slow = section.getDouble(champion, -1);
                if (isSlowdown(slow)) {
                    champions.put(champion.toLowerCase(Locale.ROOT), slow);
                } else {
                    log.warning("Ignoring zoom.champions." + champion + ": must be a slowdown above 0 and below 1");
                }
            }
        }
        return new CombatSettings(
                config.getBoolean("third-person.enabled", true),
                positive(config, "third-person.camera-distance", 3.0, log),
                config.getDouble("third-person.shoulder", 0.8),
                config.getDouble("third-person.height", 0.25),
                config.getBoolean("third-person.crosshair", true),
                config.getBoolean("zoom.enabled", true),
                slowdown(config, "zoom.melee", 0.15, log),
                slowdown(config, "zoom.ranged", 0.3, log),
                Map.copyOf(champions),
                config.getBoolean("movement.disable-sprint", true),
                config.getBoolean("movement.disable-sneak", true),
                Math.min(360, positive(config, "basic-attack.melee-angle", 100.0, log)),
                reloadKey(config.getString("basic-attack.reload-key", "sprint"), log));
    }

    /**
     * Zoom for a champion, from the player's scoreboard tags: their champion's entry in
     * zoom.champions if there is one, otherwise the melee or ranged value.
     */
    Zoom zoomFor(Set<String> tags) {
        for (String tag : tags) {
            Double slow = zoomChampions.get(tag.toLowerCase(Locale.ROOT));
            if (slow != null) return new Zoom(slow, tag);
        }
        return tags.contains("melee") ? new Zoom(zoomMelee, "melee") : new Zoom(zoomRanged, "ranged");
    }

    /** A zoom slowdown: above 0 (or it wouldn't zoom) and below 1 (or the player couldn't move). */
    static boolean isSlowdown(double value) {
        return value > 0 && value < 1;
    }

    private static double slowdown(FileConfiguration config, String path, double fallback, Logger log) {
        double value = config.getDouble(path, fallback);
        if (isSlowdown(value)) return value;
        log.warning(path + " must be above 0 and below 1, using " + fallback);
        return fallback;
    }

    private static ReloadKey reloadKey(String value, Logger log) {
        try {
            return ReloadKey.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warning("basic-attack.reload-key must be sprint, sneak or none, using sprint");
            return ReloadKey.SPRINT;
        }
    }

    private static double positive(FileConfiguration config, String path, double fallback, Logger log) {
        double value = config.getDouble(path, fallback);
        if (value > 0) return value;
        log.warning(path + " must be above 0, using " + fallback);
        return fallback;
    }
}
