package me.jberrymc.leaguecraft;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
 * tick, and it copies the player's scale. Both entities are tagged {@code unvalid}, so abilities and
 * basic attacks ignore them; a shot at the model hits the player's own (hidden) hitbox, which is in
 * the same place.
 *
 * <p>An ability can swap in its own Animated Java rig for a while (Aatrox's R):
 * {@code lc:core/model/wear} tags the rig's root {@code lc.wear} and the champion {@code lc.wearer},
 * and this mounts the root on the champion the same way, on an interaction as tall as the player
 * is (negative), since a rig's root is at its feet. The mannequin is put away until the rig is gone.
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
        /** A rig the datapack put on the player instead of the mannequin, and what it rides. */
        Entity worn;
        Interaction wornOffset;
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
        if (m.worn != null) {
            player.setInvisible(true);
            return;
        }
        if (m.offset == null || !m.offset.isValid() || m.model == null || !m.model.isValid()
                || m.offset.getWorld() != player.getWorld()) {
            despawn(m);
            spawn(player, m);
        }
        player.setInvisible(true);
    }

    /** Every tick: puts on rigs the datapack asked for, and keeps models mounted, at the right height, facing the player's way. */
    void tick() {
        wearRequested();
        for (Map.Entry<UUID, Mounted> entry : mounted.entrySet()) {
            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            Mounted m = entry.getValue();
            if (player != null && m.worn != null) {
                tickWorn(player, m);
                continue;
            }
            if (player == null || m.offset == null || !m.offset.isValid() || m.model == null || !m.model.isValid()) continue;
            // A teleport, death or world change drops passengers; put them back.
            if (m.offset.getVehicle() != player) player.addPassenger(m.offset);
            if (m.model.getVehicle() != m.offset) m.offset.addPassenger(m.model);

            double height = player.getHeight();
            if (height != m.height) {
                m.height = height;
                AttributeInstance scale = player.getAttribute(Attribute.SCALE);
                // The mannequin's drop below what it rides grows with its scale (the player's).
                m.offset.setInteractionHeight((float) (RIDER_DROP * (scale == null ? 1 : scale.getValue()) - height));
            }
            Location look = player.getLocation();
            m.model.setRotation(look.getYaw(), look.getPitch());
            m.model.setBodyYaw(player.getBodyYaw());
            AttributeInstance scale = player.getAttribute(Attribute.SCALE);
            AttributeInstance modelScale = m.model.getAttribute(Attribute.SCALE);
            if (scale != null && modelScale != null && modelScale.getBaseValue() != scale.getValue()) {
                modelScale.setBaseValue(scale.getValue());
            }
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
        unwear(m);
        player.setInvisible(m.wasInvisible);
    }

    /** Mounts rigs tagged lc.wear on the nearest champion tagged lc.wearer. */
    private void wearRequested() {
        for (Entity root : org.bukkit.Bukkit.selectEntities(org.bukkit.Bukkit.getConsoleSender(), "@e[tag=lc.wear]")) {
            Player wearer = null;
            double best = 36;
            for (Player p : root.getWorld().getPlayers()) {
                if (!p.getScoreboardTags().contains("lc.wearer")) continue;
                double d = p.getLocation().distanceSquared(root.getLocation());
                if (d < best) {
                    best = d;
                    wearer = p;
                }
            }
            if (wearer == null) continue;   // try again next tick
            root.removeScoreboardTag("lc.wear");
            wearer.removeScoreboardTag("lc.wearer");
            Mounted m = mounted.get(wearer.getUniqueId());
            if (m == null) continue;
            despawn(m);
            unwear(m);
            Player player = wearer;
            m.worn = root;
            m.wornOffset = player.getWorld().spawn(player.getLocation(), Interaction.class, i -> {
                mark(i);
                i.setInteractionWidth(0);
                i.setInteractionHeight((float) -player.getHeight());
                i.setResponsive(false);
            });
            player.addPassenger(m.wornOffset);
            m.wornOffset.addPassenger(root);
        }
    }

    /** Keeps a worn rig mounted and turned, or gives the mannequin back once the datapack removed it. */
    private void tickWorn(Player player, Mounted m) {
        if (!m.worn.isValid()) {
            unwear(m);
            return;
        }
        if (m.wornOffset == null || !m.wornOffset.isValid()) return;
        if (m.wornOffset.getVehicle() != player) player.addPassenger(m.wornOffset);
        if (m.worn.getVehicle() != m.wornOffset) m.wornOffset.addPassenger(m.worn);
        float height = (float) -player.getHeight();
        if (m.wornOffset.getInteractionHeight() != height) m.wornOffset.setInteractionHeight(height);
        m.worn.setRotation(player.getLocation().getYaw(), 0);
    }

    private static void unwear(Mounted m) {
        if (m.wornOffset != null) m.wornOffset.remove();
        m.wornOffset = null;
        m.worn = null;
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
