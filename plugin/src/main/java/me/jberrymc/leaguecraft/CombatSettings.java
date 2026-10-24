package me.jberrymc.leaguecraft;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** The third-person:, zoom:, movement:, basic-attack: and abilities: sections of config.yml, read once per load/reload. */
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
        double zoomSlowMelee,
        double zoomSlowRanged,
        Map<String, Double> zoomSlowChampions,
        boolean disableSprint,
        boolean disableSneak,
        double meleeAngle,
        ReloadKey reloadKey,
        Indicators indicators,
        GroundCast groundCast) {

    /** The most a zoom can narrow the view: the client never narrows it by half or more. */
    static final double MAX_ZOOM = 0.49;

    /** The key that reloads early (a reload also starts by itself when the ammo runs out). */
    enum ReloadKey { SPRINT, SNEAK, NONE }

    /** Which ground-targeted abilities show where they'll land: the one being aimed, or every ready one. */
    enum Indicators { AIMED, ALWAYS }

    /**
     * How a ground-targeted ability's key casts it: CONFIRM shows where it'll land on the first
     * press and casts on the second (or a left click), Shift cancels; QUICK casts on the first press.
     */
    enum GroundCast { CONFIRM, QUICK }

    /** A zoomed champion's view, how much narrower (0.3 = 30%), how much slower they walk, and why (for dev mode). */
    record Zoom(double view, double slow, String source) {}

    static CombatSettings from(FileConfiguration config, Logger log) {
        // zoom.view.champions / zoom.slowdown.champions: <champion tag> -> value
        Map<String, Double> champions = perChampion(config, "zoom.view.champions", CombatSettings::isZoom,
                "above 0 and at most " + MAX_ZOOM, log);
        Map<String, Double> slowChampions = perChampion(config, "zoom.slowdown.champions", CombatSettings::isSlowdown,
                "at least 0 and below 1", log);
        return new CombatSettings(
                config.getBoolean("third-person.enabled", true),
                positive(config, "third-person.camera-distance", 3.0, log),
                config.getDouble("third-person.shoulder", 0.8),
                config.getDouble("third-person.height", 0.25),
                config.getBoolean("third-person.crosshair", true),
                config.getBoolean("zoom.enabled", true),
                zoom(config, "zoom.view.melee", 0.15, log),
                zoom(config, "zoom.view.ranged", 0.3, log),
                Map.copyOf(champions),
                slowdown(config, "zoom.slowdown.melee", 0.2, log),
                slowdown(config, "zoom.slowdown.ranged", 0.5, log),
                Map.copyOf(slowChampions),
                config.getBoolean("movement.disable-sprint", true),
                config.getBoolean("movement.disable-sneak", true),
                Math.min(360, positive(config, "basic-attack.melee-angle", 100.0, log)),
                choice(config, "basic-attack.reload-key", ReloadKey.SPRINT, log),
                choice(config, "abilities.indicators", Indicators.AIMED, log),
                choice(config, "abilities.ground-cast", GroundCast.CONFIRM, log));
    }

    private static Map<String, Double> perChampion(FileConfiguration config, String path,
                                                   java.util.function.DoublePredicate valid, String rule, Logger log) {
        Map<String, Double> values = new HashMap<>();
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section == null) return values;
        for (String champion : section.getKeys(false)) {
            double value = section.getDouble(champion, -1);
            if (valid.test(value)) {
                values.put(champion.toLowerCase(Locale.ROOT), value);
            } else {
                log.warning("Ignoring " + path + "." + champion + ": must be " + rule);
            }
        }
        return values;
    }

    /**
     * Zoom for a champion, from the player's scoreboard tags: their champion's entry in
     * zoom.view.champions if there is one, otherwise the melee or ranged value.
     */
    Zoom zoomFor(Set<String> tags) {
        boolean melee = tags.contains("melee");
        double view = melee ? zoomMelee : zoomRanged;
        double slow = melee ? zoomSlowMelee : zoomSlowRanged;
        String source = melee ? "melee" : "ranged";
        for (String tag : tags) {
            String champion = tag.toLowerCase(Locale.ROOT);
            if (zoomChampions.containsKey(champion) || zoomSlowChampions.containsKey(champion)) {
                view = zoomChampions.getOrDefault(champion, view);
                slow = zoomSlowChampions.getOrDefault(champion, slow);
                source = tag;
            }
        }
        return new Zoom(view, slow, source);
    }

    /** A zoom slowdown: at least 0 and below 1 (or the player couldn't move). */
    static boolean isSlowdown(double value) {
        return value >= 0 && value < 1;
    }

    private static double slowdown(FileConfiguration config, String path, double fallback, Logger log) {
        double value = config.getDouble(path, fallback);
        if (isSlowdown(value)) return value;
        log.warning(path + " must be at least 0 and below 1, using " + fallback);
        return fallback;
    }

    /** A zoom: above 0 (or it wouldn't zoom) and at most {@link #MAX_ZOOM}. */
    static boolean isZoom(double value) {
        return value > 0 && value <= MAX_ZOOM;
    }

    private static double zoom(FileConfiguration config, String path, double fallback, Logger log) {
        double value = config.getDouble(path, fallback);
        if (isZoom(value)) return value;
        log.warning(path + " must be above 0 and at most " + MAX_ZOOM + ", using " + fallback);
        return fallback;
    }

    private static <E extends Enum<E>> E choice(FileConfiguration config, String path, E fallback, Logger log) {
        String value = config.getString(path, fallback.name());
        try {
            return Enum.valueOf(fallback.getDeclaringClass(), value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warning(path + " can't be " + value + ", using " + fallback.name().toLowerCase(Locale.ROOT));
            return fallback;
        }
    }

    private static double positive(FileConfiguration config, String path, double fallback, Logger log) {
        double value = config.getDouble(path, fallback);
        if (value > 0) return value;
        log.warning(path + " must be above 0, using " + fallback);
        return fallback;
    }
}
