package me.jberrymc.leaguecraft;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.logging.Logger;

/**
 * Takes sprinting and crouching away from champions, freeing Sprint and Shift for abilities.
 *
 * <p>Movement is the client's, so neither can be blocked outright; this removes what they do:
 * <ul>
 *   <li><b>Sprint:</b> Minecraft only lets players sprint with more than 6 hunger, so hunger is
 *       kept at 6 (the resource pack hides the hunger bar). The datapack no longer gives champions
 *       Saturation, which would refill it.</li>
 *   <li><b>Shift:</b> {@code sneaking_speed} is set to 1, so crouching doesn't slow you, and the
 *       server never records the crouch, so other players don't see it and the hitbox keeps its
 *       full height. Your own client still crouches in your view, and still stops you walking off
 *       edges while Shift is held.</li>
 * </ul>
 * Both keys still reach the plugin through {@code Player#getCurrentInput()}.
 *
 * <p>It also owns champions' <b>field of view</b>. The client widens or narrows the view by the
 * player's speed divided by their <i>walking speed</i>, a value only the view uses. Every tick the
 * walking speed is set to the champion's current speed, so nothing that changes their speed moves
 * the camera: not their faster base speed, not Slowness, and not the "can't move" modifiers
 * abilities use while channeling or crowd controlled. Zooming ({@link ThirdPerson#zoomView})
 * raises the walking speed instead, which narrows the view without slowing the player.
 */
final class MovementLock implements Listener {
    /** Minecraft lets players sprint only with more than this much hunger. */
    static final int NO_SPRINT_FOOD = 6;
    private static final int FULL_FOOD = 20;
    /** Bukkit's default walk speed (Minecraft's 0.1). */
    private static final float VANILLA_WALK_SPEED = 0.2f;

    private final Supplier<CombatSettings> settings;
    private final Attribute sneakingSpeed;
    private final Attribute movementSpeed;
    /** Champions this applies to right now; only they are changed and reset. */
    private final Set<UUID> locked = new HashSet<>();
    /** Bukkit's walk speed is twice Minecraft's and can't go above 1. */
    private static final double MAX_WALK_SPEED = 0.5;

    /** How much narrower each player's view should be (their zoom). */
    private final ToDoubleFunction<Player> zoom;
    /** Champions whose walking speed follows their speed. */
    private final Set<UUID> walkMatched = new HashSet<>();
    /** Ticks the walking speed stays 0 after a champion's speed changes (see matchView). */
    private static final int SETTLE_TICKS = 3;
    private final Map<UUID, Double> lastSpeed = new HashMap<>();
    private final Map<UUID, Long> settleUntil = new HashMap<>();

    private final Logger log;
    private boolean reflectionFailed;

    MovementLock(Supplier<CombatSettings> settings, ToDoubleFunction<Player> zoom, Logger log) {
        this.settings = settings;
        this.zoom = zoom;
        this.log = log;
        this.sneakingSpeed = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)
                .get(Key.key("minecraft", "sneaking_speed"));
        if (sneakingSpeed == null) {
            log.warning("This server has no minecraft:sneaking_speed attribute; crouching will still slow champions.");
        }
        this.movementSpeed = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)
                .get(Key.key("minecraft", "movement_speed"));
    }

    /**
     * At the end of every tick: sets each champion's walking speed so their view is normal, or
     * narrower by their zoom. The end of the tick, after the datapack changed their speed (casting
     * an ability roots them), so the new walking speed reaches the client with the new speed;
     * a tick apart, the client would zoom in for a moment.
     */
    @EventHandler
    public void onTickEnd(ServerTickEndEvent event) {
        for (UUID id : walkMatched) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) matchView(player);
        }
    }

    /**
     * The client's view is (speed / walking speed + 1) / 2 times the normal one, so walking speed =
     * speed keeps it normal, and speed / (1 - 2 x zoom) narrows it by the zoom. With no speed at
     * all (rooted, channeling) the client ignores the walking speed, which also keeps it normal.
     */
    private void matchView(Player player) {
        AttributeInstance speed = movementSpeed == null ? null : player.getAttribute(movementSpeed);
        if (speed == null) return;
        double current = Math.max(0, speed.getValue());
        // Minecraft sends a player's new speed only every other tick, while a walking speed goes out
        // at once; arriving apart, the client would briefly divide one by the wrong other and the
        // view would jump (a cast's root ending: speed 0 still on the client, walking speed back).
        // So when the speed changed, it's sent right now, just before the walking speed. If that
        // isn't possible, the walking speed is 0 (which the client ignores) for a few ticks, until
        // the new speed has surely arrived; a zoom pauses meanwhile.
        long now = Bukkit.getCurrentTick();
        Double last = lastSpeed.put(player.getUniqueId(), current);
        if ((last == null || Math.abs(last - current) > 1e-6) && !sendSpeedNow(player)) {
            settleUntil.put(player.getUniqueId(), now + SETTLE_TICKS);
        }
        boolean settling = now < settleUntil.getOrDefault(player.getUniqueId(), 0L);
        double ratio = 1 - 2 * Math.clamp(zoom.applyAsDouble(player), 0, CombatSettings.MAX_ZOOM);
        double walk = current < 1e-4 || settling ? 0 : current / ratio;
        if (Math.abs(player.getWalkSpeed() / 2 - walk) <= 5e-5) return;
        if (walk > MAX_WALK_SPEED && setWalkingSpeedDirectly(player, walk)) return;
        // Setting Bukkit's walk speed also sets the speed attribute's base value; put it back, so
        // only the view changes.
        double base = speed.getBaseValue();
        player.setWalkSpeed((float) (Math.min(MAX_WALK_SPEED, walk) * 2));
        speed.setBaseValue(base);
    }

    private Method getHandle;
    private Method getAttribute;
    private Method getId;
    private Method send;
    private Object speedHolder;
    private java.lang.reflect.Constructor<?> attributesPacket;
    private java.lang.reflect.Field connection;
    private boolean syncFailed;

    /**
     * Sends the player their movement speed now (ClientboundUpdateAttributesPacket), instead of at
     * Minecraft's next entity update. False if that fails (then the caller waits it out).
     */
    private boolean sendSpeedNow(Player player) {
        if (syncFailed) return false;
        try {
            if (send == null) {
                getHandle = player.getClass().getMethod("getHandle");
                Object handle = getHandle.invoke(player);
                ClassLoader loader = handle.getClass().getClassLoader();
                speedHolder = Class.forName("net.minecraft.world.entity.ai.attributes.Attributes", true, loader)
                        .getField("MOVEMENT_SPEED").get(null);
                getAttribute = handle.getClass().getMethod("getAttribute", Class.forName("net.minecraft.core.Holder", true, loader));
                getId = handle.getClass().getMethod("getId");
                attributesPacket = Class.forName("net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket", true, loader)
                        .getConstructor(int.class, Collection.class);
                connection = handle.getClass().getField("connection");
                send = connection.getType().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet", true, loader));
            }
            Object handle = getHandle.invoke(player);
            Object instance = getAttribute.invoke(handle, speedHolder);
            if (instance == null) return false;
            Object packet = attributesPacket.newInstance(getId.invoke(handle), List.of(instance));
            send.invoke(connection.get(handle), packet);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            syncFailed = true;
            log.warning("Can't send speed updates right away (" + e + "); the view waits a few ticks after speed changes instead.");
            return false;
        }
    }

    /**
     * Bukkit won't set a walk speed above its cap, which a big zoom on a fast champion needs, so
     * this sets Minecraft's own value (Abilities.walkingSpeed) and sends it. False if that fails.
     */
    private boolean setWalkingSpeedDirectly(Player player, double walk) {
        if (reflectionFailed) return false;
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object abilities = handle.getClass().getMethod("getAbilities").invoke(handle);
            java.lang.reflect.Field field = abilities.getClass().getDeclaredField("walkingSpeed");
            field.setAccessible(true);
            field.setFloat(abilities, (float) walk);
            handle.getClass().getMethod("onUpdateAbilities").invoke(handle);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            reflectionFailed = true;
            log.warning("Can't set walking speeds above Bukkit's cap (" + e + "); zooms are capped a little lower.");
            return false;
        }
    }

    /** Called every few ticks for each champion. */
    void apply(Player player, CombatSettings config) {
        walkMatched.add(player.getUniqueId());
        if (!config.disableSprint() && !config.disableSneak()) {
            unlock(player);
            return;
        }
        locked.add(player.getUniqueId());
        if (config.disableSprint()) {
            if (player.hasPotionEffect(PotionEffectType.SATURATION)) player.removePotionEffect(PotionEffectType.SATURATION);
            if (player.getFoodLevel() != NO_SPRINT_FOOD) player.setFoodLevel(NO_SPRINT_FOOD);
            if (player.getSaturation() != 0) player.setSaturation(0);
            if (player.getExhaustion() != 0) player.setExhaustion(0);
            if (player.isSprinting()) player.setSprinting(false);
        }
        AttributeInstance speed = sneakingSpeed == null ? null : player.getAttribute(sneakingSpeed);
        if (speed != null) {
            double target = config.disableSneak() ? 1.0 : speed.getDefaultValue();
            if (speed.getBaseValue() != target) speed.setBaseValue(target);
        }
    }

    /** Gives sprinting, crouching and the vanilla walking speed back once the player isn't playing a champion. */
    void remove(Player player) {
        lastSpeed.remove(player.getUniqueId());
        settleUntil.remove(player.getUniqueId());
        if (walkMatched.remove(player.getUniqueId())) {
            // Back to vanilla, keeping whatever base speed the datapack set (it resets it itself).
            AttributeInstance speed = movementSpeed == null ? null : player.getAttribute(movementSpeed);
            double base = speed == null ? 0 : speed.getBaseValue();
            player.setWalkSpeed(VANILLA_WALK_SPEED);
            if (speed != null) speed.setBaseValue(base);
        }
        unlock(player);
    }

    private void unlock(Player player) {
        if (!locked.remove(player.getUniqueId())) return;
        player.setFoodLevel(FULL_FOOD);
        AttributeInstance speed = sneakingSpeed == null ? null : player.getAttribute(sneakingSpeed);
        if (speed != null) speed.setBaseValue(speed.getDefaultValue());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && locked.contains(event.getPlayer().getUniqueId()) && settings.get().disableSneak()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSprint(PlayerToggleSprintEvent event) {
        if (event.isSprinting() && locked.contains(event.getPlayer().getUniqueId()) && settings.get().disableSprint()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }
}
