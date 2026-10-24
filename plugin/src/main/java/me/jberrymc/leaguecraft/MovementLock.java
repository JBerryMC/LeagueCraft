package me.jberrymc.leaguecraft;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
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
 */
final class MovementLock implements Listener {
    /** Minecraft lets players sprint only with more than this much hunger. */
    static final int NO_SPRINT_FOOD = 6;
    private static final int FULL_FOOD = 20;

    private final Supplier<CombatSettings> settings;
    private final Attribute sneakingSpeed;
    /** Champions this applies to right now; only they are changed and reset. */
    private final Set<UUID> locked = new HashSet<>();

    MovementLock(Supplier<CombatSettings> settings, Logger log) {
        this.settings = settings;
        this.sneakingSpeed = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)
                .get(Key.key("minecraft", "sneaking_speed"));
        if (sneakingSpeed == null) {
            log.warning("This server has no minecraft:sneaking_speed attribute; crouching will still slow champions.");
        }
    }

    /** Called every few ticks for each champion. */
    void apply(Player player, CombatSettings config) {
        if (!config.disableSprint() && !config.disableSneak()) {
            remove(player);
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

    /** Gives sprinting and crouching back once the player isn't playing a champion. */
    void remove(Player player) {
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
