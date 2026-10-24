package me.jberrymc.leaguecraft;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A model riding every champion, the way {@code /lc rigtest} mounts the Aatrox rig. Until each
 * champion has its Animated Java rig, the model is a placeholder: a mannequin wearing the player's
 * skin. The player's own body is hidden.
 *
 * <p>The mannequin can't ride the player directly: a rider sits on top of its vehicle, so it would
 * float over the player's head. It rides an interaction entity instead, which rides the player.
 * An interaction's height may be negative, and its rider sits at that height, so an interaction
 * {@code 0.6 - player height} tall (-1.2 standing) puts the mannequin's feet at the player's feet
 * (a mannequin sits 0.6 below the top of what it rides).
 *
 * <p>Riders don't turn with their vehicle, so the mannequin's rotation follows the player's every
 * tick. Both entities are tagged {@code unvalid}, so abilities and basic attacks ignore them; a
 * shot at the model hits the player's own (hidden) hitbox, which is in the same place.
 */
final class ChampionModel implements Listener {
    /** A mannequin sits this far below the top of the entity it rides. */
    private static final double RIDER_DROP = 0.6;
    static final String TAG = "lc.model";

    private final Predicate<Player> skip;
    private final Map<UUID, Mounted> mounted = new HashMap<>();

    private static final class Mounted {
        Interaction offset;
        Mannequin model;
        boolean wasInvisible;
        double height = -1;
    }

    /** {@code skip}: players who already wear another model (the rig test). */
    ChampionModel(Predicate<Player> skip) {
        this.skip = skip;
    }

    /** Called every few ticks for each champion: puts the model on if it isn't there. */
    void apply(Player player) {
        if (skip.test(player)) {
            remove(player);
            return;
        }
        Mounted m = mounted.get(player.getUniqueId());
        if (m == null) {
            m = new Mounted();
            m.wasInvisible = player.isInvisible();
            mounted.put(player.getUniqueId(), m);
        }
        if (m.offset == null || !m.offset.isValid() || m.model == null || !m.model.isValid()
                || m.offset.getWorld() != player.getWorld()) {
            despawn(m);
            spawn(player, m);
        }
        player.setInvisible(true);
    }

    /** Every tick: keeps the model mounted, at the right height, facing the player's way. */
    void tick() {
        for (Map.Entry<UUID, Mounted> entry : mounted.entrySet()) {
            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            Mounted m = entry.getValue();
            if (player == null || m.offset == null || !m.offset.isValid() || m.model == null || !m.model.isValid()) continue;
            // A teleport, death or world change drops passengers; put them back.
            if (m.offset.getVehicle() != player) player.addPassenger(m.offset);
            if (m.model.getVehicle() != m.offset) m.offset.addPassenger(m.model);

            double height = player.getHeight();
            if (height != m.height) {
                m.height = height;
                m.offset.setInteractionHeight((float) (RIDER_DROP - height));
            }
            Location look = player.getLocation();
            m.model.setRotation(look.getYaw(), look.getPitch());
            m.model.setBodyYaw(player.getBodyYaw());
            // Invisibility from abilities hides the model too.
            boolean hidden = player.hasPotionEffect(PotionEffectType.INVISIBILITY);
            if (m.model.isInvisible() != hidden) m.model.setInvisible(hidden);
        }
    }

    /** Takes the model off once the player isn't playing a champion. */
    void remove(Player player) {
        Mounted m = mounted.remove(player.getUniqueId());
        if (m == null) return;
        despawn(m);
        player.setInvisible(m.wasInvisible);
    }

    void removeAll() {
        for (UUID id : List.copyOf(mounted.keySet())) {
            Player player = org.bukkit.Bukkit.getPlayer(id);
            if (player != null) {
                remove(player);
            } else {
                despawn(mounted.remove(id));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }

    private void spawn(Player player, Mounted m) {
        Location at = player.getLocation();
        m.height = player.getHeight();
        m.offset = player.getWorld().spawn(at, Interaction.class, i -> {
            mark(i);
            i.setInteractionWidth(0);
            i.setInteractionHeight((float) (RIDER_DROP - m.height));
            i.setResponsive(false);
        });
        m.model = player.getWorld().spawn(at, Mannequin.class, mannequin -> {
            mark(mannequin);
            mannequin.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
            mannequin.setImmovable(true);
            mannequin.setInvulnerable(true);
            mannequin.setSilent(true);
            mannequin.setCollidable(false);
            mannequin.setDescription(Component.empty());
            mannequin.setRotation(at.getYaw(), at.getPitch());
        });
        player.addPassenger(m.offset);
        m.offset.addPassenger(m.model);
    }

    private static void mark(Entity entity) {
        entity.setPersistent(false);
        entity.addScoreboardTag(TAG);
        entity.addScoreboardTag("unvalid");
    }

    private static void despawn(Mounted m) {
        if (m.model != null) m.model.remove();
        if (m.offset != null) m.offset.remove();
        m.model = null;
        m.offset = null;
    }
}
