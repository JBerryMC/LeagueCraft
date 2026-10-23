package me.jberrymc.leaguecraft;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
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
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Left click: the champion's basic attack, as a {@link Hitscan} from the crosshair.
 *
 * <p>Minecraft reports a left click as one of three events, and one click can produce two of them
 * (hitting an entity also swings the arm), so each player fires at most once per tick:
 * <ul>
 *   <li>air: {@link PlayerInteractEvent} LEFT_CLICK_AIR (from the arm swing)</li>
 *   <li>a block: LEFT_CLICK_BLOCK</li>
 *   <li>an entity in vanilla reach: {@link PrePlayerAttackEntityEvent}. It's cancelled, so neither
 *       vanilla melee damage nor the datapack's "attack" interaction (its old basic attack) fires.</li>
 * </ul>
 *
 * <p>For now a hit is reported in chat; the Animated Java attack animations and damage come later.
 * With {@code basic-attack.hitscan: false} left click goes back to the datapack's basic attack.
 */
final class BasicAttackListener implements Listener {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final AbilityInputListener input;
    private final DevLog dev;
    private final Supplier<InputSettings> inputSettings;
    private final Supplier<CombatSettings> settings;
    /** Last server tick each player fired. */
    private final Map<UUID, Long> lastShot = new HashMap<>();
    /** Last server tick each player pressed drop: the client swings the arm when dropping. */
    private final Map<UUID, Long> lastDrop = new HashMap<>();

    BasicAttackListener(AbilityInputListener input, DevLog dev,
                        Supplier<InputSettings> inputSettings, Supplier<CombatSettings> settings) {
        this.input = input;
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
        String input = "left click (" + AbilityInputListener.describe(event.getAttacked()) + ")";
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
        lastShot.remove(event.getPlayer().getUniqueId());
        lastDrop.remove(event.getPlayer().getUniqueId());
    }

    /** True if left click is the hitscan attack for this player right now; otherwise says why in dev mode. */
    private boolean ready(Player player, String input) {
        if (!settings.get().hitscan()) {
            if (this.input.canCast(player, inputSettings.get())) {
                dev.passthrough(player, input, "basic-attack.hitscan is off, the datapack's basic attack handles it");
            }
            return false;
        }
        return this.input.ready(player, input, inputSettings.get());
    }

    private boolean droppedJustNow(Player player) {
        Long tick = lastDrop.get(player.getUniqueId());
        return tick != null && Bukkit.getCurrentTick() - tick <= 1;
    }

    private void fire(Player player, String input) {
        CombatSettings config = settings.get();
        long tick = Bukkit.getCurrentTick();
        Long last = lastShot.get(player.getUniqueId());
        if (last != null && tick == last) {
            dev.ignored(player, input, "duplicate event this tick (already fired)");
            return;
        }
        if (last != null && tick - last < config.cooldownTicks()) {
            dev.ignored(player, input, "basic attack on cooldown (" + (config.cooldownTicks() - (tick - last)) + " ticks left)");
            return;
        }
        lastShot.put(player.getUniqueId(), tick);

        boolean melee = player.getScoreboardTags().contains("melee");
        Hitscan.Shot shot = Hitscan.fire(player, config, melee ? config.meleeRange() : config.rangedRange());
        // Placeholder until the attack animations exist.
        player.sendMessage(describe(player, shot, melee));
        dev.shot(player, input, shot);
    }

    private static Component describe(Player shooter, Hitscan.Shot shot, boolean melee) {
        String verb = melee ? "Punch" : "Shot";
        String distance = String.format(Locale.ROOT, "%.1f", shot.distance());
        return switch (shot.kind()) {
            case ENTITY -> MM.deserialize("<gold><verb></gold> <gray>hit</gray> <white><target></white> "
                            + (ally(shooter, shot.entity()) ? "<aqua>(ally)</aqua>" : "<red>(enemy)</red>")
                            + " <dark_gray><distance>m",
                    Placeholder.unparsed("verb", verb),
                    Placeholder.component("target", shot.entity().name()),
                    Placeholder.unparsed("distance", distance));
            case BLOCK -> MM.deserialize("<gold><verb></gold> <gray>hit <white><target></white> <dark_gray><distance>m",
                    Placeholder.unparsed("verb", verb),
                    Placeholder.unparsed("target", shot.block().getType().getKey().getKey()),
                    Placeholder.unparsed("distance", distance));
            case MISS -> MM.deserialize("<gold><verb></gold> <gray>missed <dark_gray>(<distance>m range)",
                    Placeholder.unparsed("verb", verb),
                    Placeholder.unparsed("distance", String.format(Locale.ROOT, "%.1f", shot.range())));
        };
    }

    /** Same scoreboard team as the shooter (the datapack's red and blue teams). */
    private static boolean ally(Player shooter, Entity target) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getEntryTeam(shooter.getName());
        if (team == null) return false;
        String entry = target instanceof Player player ? player.getName() : target.getUniqueId().toString();
        return team.equals(board.getEntryTeam(entry));
    }

}
