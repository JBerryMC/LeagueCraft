package me.jberrymc.leaguecraft;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.function.Predicate;

/**
 * Instant-hit basic attacks aimed with the ShoulderCam crosshair.
 *
 * <p>Two traces, the way third-person shooters do it:
 * <ol>
 *   <li><b>Aim:</b> along the crosshair ray (see {@link ShoulderCamera}) to find what the player is
 *       pointing at.</li>
 *   <li><b>Shot:</b> from the player's eye to that point. This is what actually hits, so the
 *       player can't shoot round a corner that only the camera can see past.</li>
 * </ol>
 *
 * <p>Targets follow the datapack's own basic attack rules ({@code _champion:aa/melee}): any entity
 * except the {@code #minecraft:unvalid} types and entities tagged {@code unvalid}, plus the
 * interaction entities of structures (tag {@code lol.structure}, the turrets and Nexus).
 */
final class Hitscan {
    /** Entity types the datapack never treats as targets (data/minecraft/tags/entity_type/unvalid.json). */
    private static final NamespacedKey UNVALID = NamespacedKey.minecraft("unvalid");

    enum Kind { ENTITY, BLOCK, MISS }

    /**
     * What a shot did. {@code entity} is set for ENTITY and {@code block} for BLOCK; {@code hit} is
     * where it landed (the end of the range for a miss), and {@code distance} how far that is from the eye.
     */
    record Shot(Kind kind, Entity entity, Block block, Vec3 hit, double distance,
                double range, Vec3 aimOrigin, Vec3 aimPoint) {}

    private Hitscan() {}

    static Shot fire(Player player, CombatSettings settings, double range) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vec3 eye = vec(eyeLocation.toVector());
        float yaw = eyeLocation.getYaw();
        float pitch = eyeLocation.getPitch();
        Vec3 forward = ShoulderCamera.forward(yaw, pitch);
        Predicate<Entity> targets = entity -> isTarget(player, entity);

        Vec3 aimOrigin = ShoulderCamera.aimOrigin(eye, yaw, pitch, settings.shoulder(), settings.height());
        // Standing with a wall beside you puts the crosshair ray inside the wall. Start it where the
        // wall begins instead, so the shot still goes forward.
        Vec3 toOrigin = aimOrigin.subtract(eye);
        if (toOrigin.length() > 1e-6) {
            RayTraceResult wall = world.rayTraceBlocks(eyeLocation, bukkit(toOrigin.normalize(forward)),
                    toOrigin.length(), FluidCollisionMode.NEVER, true);
            if (wall != null) aimOrigin = eye.add(vec(wall.getHitPosition()).subtract(eye).multiply(0.9));
        }

        // 1. Aim: what's under the crosshair.
        RayTraceResult aim = world.rayTrace(location(world, aimOrigin), bukkit(forward), range,
                FluidCollisionMode.NEVER, true, 0.0, targets);
        Vec3 aimPoint = aim != null ? vec(aim.getHitPosition()) : aimOrigin.add(forward.multiply(range));

        // 2. Shot: from the eye to that point. Melee keeps its range from the eye, not the camera.
        Vec3 toAim = aimPoint.subtract(eye);
        double length = Math.min(toAim.length(), range) + 0.01;
        Vec3 direction = toAim.normalize(forward);
        RayTraceResult shot = world.rayTrace(eyeLocation, bukkit(direction), length,
                FluidCollisionMode.NEVER, true, 0.0, targets);

        if (shot == null) {
            Vec3 end = eye.add(direction.multiply(range));
            return new Shot(Kind.MISS, null, null, end, range, range, aimOrigin, aimPoint);
        }
        Vec3 hit = vec(shot.getHitPosition());
        double distance = hit.subtract(eye).length();
        if (shot.getHitEntity() != null) {
            return new Shot(Kind.ENTITY, shot.getHitEntity(), null, hit, distance, range, aimOrigin, aimPoint);
        }
        return new Shot(Kind.BLOCK, null, shot.getHitBlock(), hit, distance, range, aimOrigin, aimPoint);
    }

    static boolean isTarget(Player shooter, Entity entity) {
        if (entity.equals(shooter) || entity.isDead()) return false;
        if (shooter.getPassengers().contains(entity) || entity.getPassengers().contains(shooter)) return false;
        if (entity instanceof Interaction) return entity.getScoreboardTags().contains("lol.structure");
        if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) return false;
        if (entity.getScoreboardTags().contains("unvalid")) return false;
        Tag<EntityType> unvalid = Bukkit.getTag(Tag.REGISTRY_ENTITY_TYPES, UNVALID, EntityType.class);
        // Without the datapack, fall back to anything alive.
        return unvalid != null ? !unvalid.isTagged(entity.getType()) : entity instanceof LivingEntity;
    }

    private static Vec3 vec(Vector v) {
        return new Vec3(v.getX(), v.getY(), v.getZ());
    }

    private static Vector bukkit(Vec3 v) {
        return new Vector(v.x(), v.y(), v.z());
    }

    private static Location location(World world, Vec3 v) {
        return new Location(world, v.x(), v.y(), v.z());
    }
}
