package me.jberrymc.leaguecraft;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * What a player's third-person crosshair points at, for skill shots.
 *
 * <p>Asks ShoulderCam ({@code ShoulderCam.aim().target(player, range, filter)}) when it's installed:
 * it knows each player's camera mode (the shader camera, the Shoulder Surfing mod, or none) and aims
 * to match. It's called by reflection, so LeagueCraft builds and runs without ShoulderCam; without
 * it, the aim is LeagueCraft's own crosshair ray ({@link Hitscan#aim}), which assumes the shader
 * camera.
 */
final class AimPoint {
    /** How far skill shots look for something to aim at. */
    static final double RANGE = 64;

    private final Supplier<CombatSettings> settings;
    private final Logger log;
    private Method aimService;
    private Method target;
    private boolean shoulderCamFailed;

    AimPoint(Supplier<CombatSettings> settings, Logger log) {
        this.settings = settings;
        this.log = log;
    }

    /** The point the crosshair is on (an enemy, a block, or the end of the range). */
    Location find(Player player) {
        Predicate<Entity> targets = entity -> Hitscan.isTarget(player, entity);
        Vector point = fromShoulderCam(player, targets);
        if (point == null) {
            Vec3 aim = Hitscan.aim(player, settings.get(), RANGE).point();
            point = new Vector(aim.x(), aim.y(), aim.z());
        }
        return point.toLocation(player.getWorld());
    }

    /** ShoulderCam's aim, or null if it isn't installed (or its API isn't what we expect). */
    private Vector fromShoulderCam(Player player, Predicate<Entity> targets) {
        if (shoulderCamFailed) return null;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ShoulderCam");
        if (plugin == null || !plugin.isEnabled()) return null;
        try {
            if (target == null) {
                Class<?> api = Class.forName("me.jberrymc.shouldercam.ShoulderCam", true, plugin.getClass().getClassLoader());
                aimService = api.getMethod("aim");
                target = aimService.getReturnType().getMethod("target", Player.class, double.class, Predicate.class);
            }
            return (Vector) target.invoke(aimService.invoke(null), player, RANGE, targets);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            shoulderCamFailed = true;
            log.warning("Couldn't use ShoulderCam's aim API (" + e + "); skill shots use LeagueCraft's own crosshair aim.");
            return null;
        }
    }
}
