package me.jberrymc.leaguecraft;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

/** The third-person: and basic-attack: sections of config.yml, read once per load/reload. */
record CombatSettings(
        boolean thirdPerson,
        double cameraDistance,
        double shoulder,
        double height,
        boolean crosshair,
        boolean hitscan,
        double meleeRange,
        double rangedRange,
        int cooldownTicks) {

    static CombatSettings from(FileConfiguration config, Logger log) {
        return new CombatSettings(
                config.getBoolean("third-person.enabled", true),
                positive(config, "third-person.camera-distance", 3.0, log),
                config.getDouble("third-person.shoulder", 0.8),
                config.getDouble("third-person.height", 0.25),
                config.getBoolean("third-person.crosshair", true),
                config.getBoolean("basic-attack.hitscan", true),
                positive(config, "basic-attack.melee-range", 3.5, log),
                positive(config, "basic-attack.ranged-range", 48.0, log),
                Math.max(0, config.getInt("basic-attack.cooldown-ticks", 10)));
    }

    private static double positive(FileConfiguration config, String path, double fallback, Logger log) {
        double value = config.getDouble(path, fallback);
        if (value > 0) return value;
        log.warning(path + " must be above 0, using " + fallback);
        return fallback;
    }
}
