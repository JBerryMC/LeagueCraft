package me.jberrymc.leaguecraft;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Sets velocities the datapack asks for, so dashes and knockups move players directly instead of
 * mounting them on another entity (a datapack can't set a player's velocity, the API can).
 *
 * <p>The datapack ({@code lc:core/motion}) leaves a request on any entity: the scores
 * {@code motion.x/y/z} in thousandths of a block per tick, {@code motion.ticks}, and the tag
 * {@code lc.motion}. With {@code motion.ticks} above 1 the velocity is set again every tick for that
 * many ticks, so the entity flies straight; the tag {@code lc.motion.stop} cancels that. A new
 * request replaces a held one.
 *
 * <p>Requests are handled when the tick ends, after the datapack ran, so a velocity reaches the
 * client in the same tick it was asked for. Dashes ask every tick (the player follows the dash
 * entity, {@code motion.ticks} -1), which keeps them on the dash's path. The first tick without a
 * follow request means the dash is over, and the entity stops dead, as dismounting used to do;
 * otherwise the last step's speed would carry it on, far in the air.
 */
final class Motion implements Listener {
    private static final String REQUEST = "lc.motion";
    private static final String STOP = "lc.motion.stop";
    /** Set by lc:core/motion/follow while an entity follows a dash entity. */
    private static final String FOLLOWING = "lc.following";

    private final Datapack datapack;
    /** Entities whose velocity is held, and for how many more ticks. */
    private final Map<UUID, Held> held = new HashMap<>();
    /** Entities that followed a dash entity last tick. */
    private Set<UUID> following = new HashSet<>();

    private record Held(Vector velocity, int ticksLeft) {}

    Motion(Datapack datapack) {
        this.datapack = datapack;
    }

    @EventHandler
    public void onTickEnd(ServerTickEndEvent event) {
        for (Entity entity : select(STOP)) {
            held.remove(entity.getUniqueId());
            entity.removeScoreboardTag(STOP);
            entity.removeScoreboardTag(FOLLOWING);
        }
        Map<UUID, Held> started = new HashMap<>();
        Set<UUID> followed = new HashSet<>();
        Set<UUID> requested = new HashSet<>();
        for (Entity entity : select(REQUEST)) {
            entity.removeScoreboardTag(REQUEST);
            requested.add(entity.getUniqueId());
            Vector velocity = new Vector(datapack.score(entity, "motion.x", 0), datapack.score(entity, "motion.y", 0),
                    datapack.score(entity, "motion.z", 0)).multiply(0.001);
            entity.setVelocity(velocity);
            int ticks = datapack.score(entity, "motion.ticks", 0);
            held.remove(entity.getUniqueId());
            if (ticks > 1) started.put(entity.getUniqueId(), new Held(velocity, ticks - 1));
            if (ticks < 0) followed.add(entity.getUniqueId());
        }
        // A dash that wasn't followed this tick is over: stop dead, unless something else (a
        // knockup) was asked for this tick.
        for (UUID id : following) {
            if (followed.contains(id)) continue;
            Entity entity = Bukkit.getEntity(id);
            if (entity == null) continue;
            entity.removeScoreboardTag(FOLLOWING);
            if (!requested.contains(id) && !held.containsKey(id)) entity.setVelocity(new Vector());
        }
        following = followed;
        for (Iterator<Map.Entry<UUID, Held>> it = held.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Held> entry = it.next();
            Entity entity = Bukkit.getEntity(entry.getKey());
            Held h = entry.getValue();
            if (entity == null || !entity.isValid()) {
                it.remove();
                continue;
            }
            entity.setVelocity(h.velocity());
            if (h.ticksLeft() <= 1) {
                it.remove();
            } else {
                entry.setValue(new Held(h.velocity(), h.ticksLeft() - 1));
            }
        }
        held.putAll(started);
    }

    /** Every entity with this tag, in every world. */
    private static List<Entity> select(String tag) {
        return Bukkit.selectEntities(Bukkit.getConsoleSender(), "@e[tag=" + tag + "]");
    }
}
