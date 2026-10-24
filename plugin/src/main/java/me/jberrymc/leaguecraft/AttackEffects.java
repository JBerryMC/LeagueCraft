package me.jberrymc.leaguecraft;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Particles for basic attacks: a trail from the weapon to where a shot landed, and a sweep in front
 * of a melee attack with a burst on everything it hit.
 *
 * <p>Each ranged champion's trail is the particle its old homing projectile left
 * ({@code lc:champion/aa/ranged.particle}).
 */
final class AttackEffects {
    /** A particle and its data (null if the particle takes none). */
    private record Trail(Particle particle, Object data) {}

    private static final Map<String, Trail> TRAILS = Map.of(
            "ashe", new Trail(Particle.DUST, new Particle.DustOptions(Color.fromRGB(128, 230, 255), 0.8f)),
            "jhin", new Trail(Particle.DUST, new Particle.DustOptions(Color.fromRGB(128, 230, 255), 0.8f)),
            "ezreal", new Trail(Particle.DUST, new Particle.DustOptions(Color.fromRGB(255, 244, 141), 0.8f)),
            "vayne", new Trail(Particle.CRIT, null),
            "soraka", new Trail(Particle.END_ROD, null),
            "lux", new Trail(Particle.ELECTRIC_SPARK, null),
            "annie", new Trail(Particle.FLAME, null));
    private static final Trail DEFAULT_TRAIL = new Trail(Particle.CRIT, null);
    /** Blocks between trail particles. */
    private static final double TRAIL_STEP = 0.35;
    private static final int MAX_TRAIL_PARTICLES = 120;

    private AttackEffects() {}

    /** A ranged shot: a trail from beside the weapon to where it landed, and a burst where it hit. */
    static void shot(Player player, Hitscan.Shot shot) {
        World world = player.getWorld();
        Trail trail = trail(player.getScoreboardTags());
        Vec3 eye = Hitscan.vec(player.getEyeLocation().toVector());
        Vec3 end = shot.hit();
        // Start at the weapon hand: a little right of and below the eye, and in front of it.
        Vec3 forward = end.subtract(eye).normalize(ShoulderCamera.forward(player.getYaw(), player.getPitch()));
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize(new Vec3(1, 0, 0));
        Vec3 start = eye.add(forward.multiply(0.6)).add(right.multiply(0.35)).add(new Vec3(0, -0.3, 0));
        Vec3 path = end.subtract(start);
        int steps = (int) Math.min(MAX_TRAIL_PARTICLES, Math.ceil(path.length() / TRAIL_STEP));
        for (int i = 0; i <= steps; i++) {
            Vec3 at = start.add(path.multiply(steps == 0 ? 0 : (double) i / steps));
            spawn(world, trail, at, 1, 0);
        }
        switch (shot.kind()) {
            case ENTITY -> world.spawnParticle(Particle.CRIT, Hitscan.location(world, end), 10, 0.15, 0.15, 0.15, 0.25);
            case BLOCK -> world.spawnParticle(Particle.BLOCK, Hitscan.location(world, end), 8, 0.1, 0.1, 0.1, 0,
                    shot.block().getBlockData());
            case MISS -> { }
        }
    }

    /** A melee attack: a sweep in front, and a burst on each target hit. */
    static void melee(Player player, Hitscan.Aim aim, List<Entity> hits) {
        World world = player.getWorld();
        Vec3 sweep = aim.eye().add(aim.direction().multiply(1.6)).add(new Vec3(0, -0.2, 0));
        world.spawnParticle(Particle.SWEEP_ATTACK, Hitscan.location(world, sweep), 1, 0, 0, 0, 0);
        for (Entity hit : hits) {
            BoundingBox box = hit.getBoundingBox();
            world.spawnParticle(Particle.CRIT, Hitscan.location(world, Hitscan.vec(box.getCenter())), 12,
                    box.getWidthX() / 4, box.getHeight() / 4, box.getWidthZ() / 4, 0.3);
        }
    }

    private static Trail trail(Set<String> tags) {
        for (Map.Entry<String, Trail> entry : TRAILS.entrySet()) {
            if (tags.contains(entry.getKey())) return entry.getValue();
        }
        return DEFAULT_TRAIL;
    }

    private static void spawn(World world, Trail trail, Vec3 at, int count, double speed) {
        if (trail.data() == null) {
            world.spawnParticle(trail.particle(), Hitscan.location(world, at), count, 0, 0, 0, speed);
        } else {
            world.spawnParticle(trail.particle(), Hitscan.location(world, at), count, 0, 0, 0, speed, trail.data());
        }
    }
}
