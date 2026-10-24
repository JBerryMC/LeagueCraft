package me.jberrymc.leaguecraft;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Where a ground-targeted ability lands, the way third-person hero shooters aim them: along the
 * crosshair ray, onto the ground.
 * <ul>
 *   <li>The crosshair ray stops at the first block it meets, so a target is never behind a wall.</li>
 *   <li>Hitting the top of a block: the target is right there.</li>
 *   <li>Hitting a wall or a ceiling: the target drops straight down from just in front of it, to
 *       the floor (never onto the ceiling or into the wall).</li>
 *   <li>Hitting nothing within range: the target drops straight down from the end of the range.</li>
 *   <li>No floor below within {@link #MAX_DROP} blocks: no target (the ability isn't cast).</li>
 * </ul>
 * How far each ability reaches is the {@code aim} table of the datapack's stats chart.
 */
final class GroundTarget {
    /** How far a target may drop to find the floor. */
    static final double MAX_DROP = 32;
    /** How far a target backs away from the wall or ceiling it hit, before dropping. */
    private static final double BACK_OFF = 0.3;

    private GroundTarget() {}

    /** The point on the floor this player is aiming at, at most {@code range} along the crosshair; null if there's no floor. */
    static Location find(Player player, CombatSettings settings, double range) {
        World world = player.getWorld();
        Location eye = player.getEyeLocation();
        Vec3 forward = ShoulderCamera.forward(eye.getYaw(), eye.getPitch());
        Vec3 origin = ShoulderCamera.aimOrigin(Hitscan.vec(eye.toVector()), eye.getYaw(), eye.getPitch(),
                settings.shoulder(), settings.height());
        // A wall beside the player puts the crosshair ray inside it; start where the wall begins.
        Vec3 toOrigin = origin.subtract(Hitscan.vec(eye.toVector()));
        if (toOrigin.length() > 1e-6) {
            RayTraceResult wall = world.rayTraceBlocks(eye, Hitscan.bukkit(toOrigin.normalize(forward)),
                    toOrigin.length(), FluidCollisionMode.NEVER, true);
            if (wall != null) origin = Hitscan.vec(eye.toVector()).add(Hitscan.vec(wall.getHitPosition()).subtract(Hitscan.vec(eye.toVector())).multiply(0.9));
        }
        RayTraceResult hit = world.rayTraceBlocks(Hitscan.location(world, origin), Hitscan.bukkit(forward), range,
                FluidCollisionMode.NEVER, true);
        Vec3 drop;
        if (hit == null) {
            drop = origin.add(forward.multiply(range));
        } else {
            Vec3 at = Hitscan.vec(hit.getHitPosition());
            if (hit.getHitBlockFace() == BlockFace.UP) return Hitscan.location(world, at).add(0, 0.01, 0);
            // A wall or ceiling: back away from it, then drop.
            BlockFace face = hit.getHitBlockFace();
            Vector normal = face == null ? Hitscan.bukkit(forward.multiply(-1)) : face.getDirection();
            drop = at.add(Hitscan.vec(normal).multiply(BACK_OFF));
        }
        RayTraceResult floor = world.rayTraceBlocks(Hitscan.location(world, drop), new Vector(0, -1, 0), MAX_DROP,
                FluidCollisionMode.NEVER, true);
        if (floor == null || floor.getHitBlockFace() != BlockFace.UP) return null;
        return Hitscan.location(world, Hitscan.vec(floor.getHitPosition())).add(0, 0.01, 0);
    }
}
