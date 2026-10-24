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
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * <p>Melee attacks use the same aim, but hit every target in a cone around it ({@link #cone}).
 *
 * <p>Targets follow the datapack's own basic attack rules ({@code lc:champion/aa/melee}): enemies
 * only, any entity except the {@code #minecraft:unvalid} types and entities tagged {@code unvalid},
 * plus the interaction entities of enemy structures (tag {@code lol.structure}, the turrets and
 * Nexus). Allies don't block shots.
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

    /** Where the crosshair points: the aim ray's start and end, and the direction from the eye to the end. */
    record Aim(Vec3 eye, Vec3 origin, Vec3 point, Vec3 direction) {}

    private Hitscan() {}

    static Shot fire(Player player, Aim aim, double range) {
        World world = player.getWorld();
        Vec3 eye = aim.eye();
        Vec3 direction = aim.direction();

        // 2. Shot: from the eye to the aim point. Range counts from the eye, not the camera.
        double length = Math.min(aim.point().subtract(eye).length(), range) + 0.01;
        RayTraceResult shot = world.rayTrace(location(world, eye), bukkit(direction), length,
                FluidCollisionMode.NEVER, true, 0.0, entity -> isTarget(player, entity));

        if (shot == null) {
            Vec3 end = eye.add(direction.multiply(range));
            return new Shot(Kind.MISS, null, null, end, range, range, aim.origin(), aim.point());
        }
        Vec3 hit = vec(shot.getHitPosition());
        double distance = hit.subtract(eye).length();
        if (shot.getHitEntity() != null) {
            return new Shot(Kind.ENTITY, shot.getHitEntity(), null, hit, distance, range, aim.origin(), aim.point());
        }
        return new Shot(Kind.BLOCK, null, shot.getHitBlock(), hit, distance, range, aim.origin(), aim.point());
    }

    /**
     * Every target within {@code range} of the eye and within {@code angle} degrees (the full width)
     * of the aim, that the player can see. Nearest first.
     */
    static List<Entity> cone(Player player, Aim aim, double range, double angle) {
        World world = player.getWorld();
        Vec3 eye = aim.eye();
        double minCos = Math.cos(Math.toRadians(angle / 2));
        Map<Entity, Double> reach = new HashMap<>();
        for (Entity entity : world.getNearbyEntities(location(world, eye), range + 2, range + 2, range + 2,
                entity -> isTarget(player, entity))) {
            BoundingBox box = entity.getBoundingBox();
            // The point of the hitbox nearest the eye decides the range, so big hitboxes
            // (structures) count as soon as any part of them is in reach.
            Vec3 nearest = new Vec3(clamp(eye.x(), box.getMinX(), box.getMaxX()),
                    clamp(eye.y(), box.getMinY(), box.getMaxY()), clamp(eye.z(), box.getMinZ(), box.getMaxZ()));
            double distance = nearest.subtract(eye).length();
            if (distance > range) continue;
            Vec3 toCenter = vec(box.getCenter()).subtract(eye);
            boolean inCone = distance < 1e-3 || cos(aim.direction(), toCenter) >= minCos
                    || cos(aim.direction(), nearest.subtract(eye)) >= minCos;
            if (!inCone) continue;
            // No hitting through walls.
            if (toCenter.length() > 0.5 && world.rayTraceBlocks(location(world, eye),
                    bukkit(toCenter.normalize(aim.direction())), toCenter.length() - 0.3,
                    FluidCollisionMode.NEVER, true) != null) continue;
            reach.put(entity, distance);
        }
        List<Entity> hits = new ArrayList<>(reach.keySet());
        hits.sort(Comparator.comparingDouble(reach::get));
        return hits;
    }

    /** 1. Aim: finds what's under the crosshair, at most {@code range} away. */
    static Aim aim(Player player, CombatSettings settings, double range) {
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

        RayTraceResult aim = world.rayTrace(location(world, aimOrigin), bukkit(forward), range,
                FluidCollisionMode.NEVER, true, 0.0, targets);
        Vec3 aimPoint = aim != null ? vec(aim.getHitPosition()) : aimOrigin.add(forward.multiply(range));
        return new Aim(eye, aimOrigin, aimPoint, aimPoint.subtract(eye).normalize(forward));
    }

    static boolean isTarget(Player shooter, Entity entity) {
        if (entity.equals(shooter) || entity.isDead()) return false;
        if (ridesOn(entity, shooter) || ridesOn(shooter, entity)) return false;
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getEntryTeam(shooter.getName());
        if (entity instanceof Interaction) {
            // Structures carry their team as a tag, not as team membership.
            return entity.getScoreboardTags().contains("lol.structure")
                    && (team == null || !entity.getScoreboardTags().contains(team.getName()));
        }
        if (entity instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) return false;
        if (entity.getScoreboardTags().contains("unvalid")) return false;
        String entry = entity instanceof Player player ? player.getName() : entity.getUniqueId().toString();
        if (team != null && team.hasEntry(entry)) return false;
        Tag<EntityType> unvalid = Bukkit.getTag(Tag.REGISTRY_ENTITY_TYPES, UNVALID, EntityType.class);
        // Without the datapack, fall back to anything alive.
        return unvalid != null ? !unvalid.isTagged(entity.getType()) : entity instanceof LivingEntity;
    }

    /** True if {@code passenger} rides {@code vehicle}, directly or on top of other passengers. */
    private static boolean ridesOn(Entity passenger, Entity vehicle) {
        for (Entity v = passenger.getVehicle(); v != null; v = v.getVehicle()) {
            if (v.equals(vehicle)) return true;
        }
        return false;
    }

    private static double cos(Vec3 a, Vec3 b) {
        double length = a.length() * b.length();
        return length < 1e-9 ? 1 : (a.x() * b.x() + a.y() * b.y() + a.z() * b.z()) / length;
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    static Vec3 vec(Vector v) {
        return new Vec3(v.getX(), v.getY(), v.getZ());
    }

    static Vector bukkit(Vec3 v) {
        return new Vector(v.x(), v.y(), v.z());
    }

    static Location location(World world, Vec3 v) {
        return new Location(world, v.x(), v.y(), v.z());
    }
}
