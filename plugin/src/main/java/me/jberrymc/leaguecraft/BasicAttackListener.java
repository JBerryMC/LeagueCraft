package me.jberrymc.leaguecraft;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Left click: the champion's basic attack, aimed with the crosshair.
 *
 * <ul>
 *   <li><b>Melee</b> champions (datapack tag {@code melee}) hit every enemy in a cone in front
 *       ({@code basic-attack.melee-angle} wide), within their range.</li>
 *   <li><b>Ranged</b> champions hit the first enemy along the crosshair ({@link Hitscan}), and use
 *       {@link Ammo}.</li>
 * </ul>
 * Range and attack speed are stats from the datapack's stats chart (the {@code aa.range} and
 * {@code aa.spd} scores), so buffs that change them work too. Each target takes the attacker's
 * basic attack through the datapack's damage system, with every on-hit effect
 * ({@code lc:champion/aa/plugin.hit}).
 *
 * <p>Minecraft reports a left click as one of three events, and one click can produce two of them
 * (hitting an entity also swings the arm), so each player attacks at most once per tick:
 * <ul>
 *   <li>air: {@link PlayerInteractEvent} LEFT_CLICK_AIR (from the arm swing)</li>
 *   <li>a block: LEFT_CLICK_BLOCK</li>
 *   <li>an entity in vanilla reach: {@link PrePlayerAttackEntityEvent}. It's cancelled, so vanilla
 *       melee damage never applies.</li>
 * </ul>
 */
final class BasicAttackListener implements Listener {
    private static final double FALLBACK_MELEE_RANGE = 3.5;
    private static final double FALLBACK_RANGED_RANGE = 25;
    /** Attacks per second x10, the datapack's aa.spd. */
    private static final int FALLBACK_ATTACK_SPEED = 10;

    private final AbilityInputListener input;
    private final Datapack datapack;
    private final Ammo ammo;
    private final DevLog dev;
    private final Supplier<InputSettings> inputSettings;
    private final Supplier<CombatSettings> settings;
    /** Last server tick each player attacked. */
    private final Map<UUID, Long> lastShot = new HashMap<>();
    /** First server tick each player may attack again. */
    private final Map<UUID, Long> nextShot = new HashMap<>();
    /** Last server tick each player pressed drop: the client swings the arm when dropping. */
    private final Map<UUID, Long> lastDrop = new HashMap<>();

    BasicAttackListener(AbilityInputListener input, Datapack datapack, Ammo ammo, DevLog dev,
                        Supplier<InputSettings> inputSettings, Supplier<CombatSettings> settings) {
        this.input = input;
        this.datapack = datapack;
        this.ammo = ammo;
        this.dev = dev;
        this.inputSettings = inputSettings;
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onClick(PlayerInteractEvent event) {
        // Left-clicking air arrives already cancelled, so no ignoreCancelled here.
        Action click = event.getAction();
        if (click != Action.LEFT_CLICK_AIR && click != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        String input = "left click (" + (click == Action.LEFT_CLICK_AIR ? "air" : "block") + ")";
        if (click == Action.LEFT_CLICK_AIR && droppedJustNow(player)) {
            dev.ignored(player, input, "arm swing from the drop key, not a click");
            return;
        }
        if (!ready(player, input)) return;
        // Don't start breaking the block.
        if (click == Action.LEFT_CLICK_BLOCK) event.setCancelled(true);
        fire(player, input);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAttack(PrePlayerAttackEntityEvent event) {
        Player player = event.getPlayer();
        String input = "left click (" + event.getAttacked().getType().getKey().getKey() + ")";
        if (!ready(player, input)) return;
        event.setCancelled(true);
        fire(player, input);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent event) {
        // MONITOR without ignoreCancelled: the ability listener cancels drops of ability items.
        lastDrop.put(event.getPlayer().getUniqueId(), (long) Bukkit.getCurrentTick());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastShot.remove(id);
        nextShot.remove(id);
        lastDrop.remove(id);
    }

    /** True if this player can attack right now; otherwise says why in dev mode. */
    private boolean ready(Player player, String input) {
        return this.input.ready(player, input, inputSettings.get());
    }

    private boolean droppedJustNow(Player player) {
        Long tick = lastDrop.get(player.getUniqueId());
        return tick != null && Bukkit.getCurrentTick() - tick <= 1;
    }

    private void fire(Player player, String input) {
        CombatSettings config = settings.get();
        UUID id = player.getUniqueId();
        long tick = Bukkit.getCurrentTick();
        Long last = lastShot.get(id);
        if (last != null && tick == last) {
            dev.ignored(player, input, "duplicate event this tick (already attacked)");
            return;
        }
        // Left click while aiming a ground-targeted ability casts it instead of attacking.
        if (this.input.confirmAim(player, input)) {
            lastShot.put(id, tick);
            return;
        }
        Set<String> tags = player.getScoreboardTags();
        if (tags.contains("cced") || tags.contains("airborne")) {
            dev.ignored(player, input, "crowd controlled");
            return;
        }
        if (datapack.score(player, "channeling", 0) > 0) {
            dev.ignored(player, input, "channeling an ability");
            return;
        }
        Long next = nextShot.get(id);
        if (next != null && tick < next) {
            dev.ignored(player, input, "basic attack on cooldown (" + (next - tick) + " ticks left)");
            return;
        }
        if (!ammo.take(player, input)) return;
        boolean melee = tags.contains("melee");
        lastShot.put(id, tick);
        nextShot.put(id, tick + cooldownTicks(player));

        int rangeScore = datapack.score(player, "aa.range", -1);
        double range = rangeScore > 0 ? rangeScore / 10.0 : melee ? FALLBACK_MELEE_RANGE : FALLBACK_RANGED_RANGE;
        Hitscan.Aim aim = Hitscan.aim(player, config, range);
        datapack.attack(player);
        if (melee) {
            List<Entity> hits = Hitscan.cone(player, aim, range, config.meleeAngle());
            for (Entity hit : hits) datapack.hit(player, hit);
            AttackEffects.melee(player, aim, hits);
            dev.melee(player, input, hits, range);
        } else {
            Hitscan.Shot shot = Hitscan.fire(player, aim, range);
            if (shot.kind() == Hitscan.Kind.ENTITY) datapack.hit(player, shot.entity());
            AttackEffects.shot(player, shot);
            dev.shot(player, input, shot);
        }
    }

    /**
     * Ticks between attacks, the way the datapack's own basic attack counted them
     * ({@code lc:champion/aa/cd}): 2 x (100 / aa.spd), doubled while crippled.
     */
    private int cooldownTicks(Player player) {
        int speed = Math.max(1, datapack.score(player, "aa.spd", FALLBACK_ATTACK_SPEED));
        int ticks = Math.max(1, 2 * (100 / speed));
        return datapack.score(player, "cc.cripple", -1) >= 0 ? ticks * 2 : ticks;
    }
}
