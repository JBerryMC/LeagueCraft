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
        double meleeRange,
        double rangedRange,
        int cooldownTicks) {

    /** Camera distance while zoomed, and why (for dev mode). */
    record Zoom(double distance, String source) {}

    static CombatSettings from(FileConfiguration config, Logger log) {
        // zoom.champions: <champion tag> -> camera distance
        Map<String, Double> champions = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection("zoom.champions");
        if (section != null) {
            for (String champion : section.getKeys(false)) {
                double distance = section.getDouble(champion, -1);
                if (distance > 0) {
                    champions.put(champion.toLowerCase(Locale.ROOT), distance);
                } else {
                    log.warning("Ignoring zoom.champions." + champion + ": must be a camera distance above 0");
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
                positive(config, "zoom.melee", 2.2, log),
                positive(config, "zoom.ranged", 1.2, log),
                Map.copyOf(champions),
                config.getBoolean("movement.disable-sprint", true),
                config.getBoolean("movement.disable-sneak", true),
                positive(config, "basic-attack.melee-range", 3.5, log),
                positive(config, "basic-attack.ranged-range", 48.0, log),
                Math.max(0, config.getInt("basic-attack.cooldown-ticks", 10)));
    }

    /**
     * Zoom for a champion, from the player's scoreboard tags: their champion's entry in
     * zoom.champions if there is one, otherwise the melee or ranged value.
     */
    Zoom zoomFor(Set<String> tags) {
        for (String tag : tags) {
            Double distance = zoomChampions.get(tag.toLowerCase(Locale.ROOT));
            if (distance != null) return new Zoom(distance, tag);
        }
        return tags.contains("melee") ? new Zoom(zoomMelee, "melee") : new Zoom(zoomRanged, "ranged");
    }

    private static double positive(FileConfiguration config, String path, double fallback, Logger log) {
        double value = config.getDouble(path, fallback);
        if (value > 0) return value;
        log.warning(path + " must be above 0, using " + fallback);
        return fallback;
    }
}
