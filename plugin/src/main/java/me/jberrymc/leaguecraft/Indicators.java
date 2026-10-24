package me.jberrymc.leaguecraft;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Function;

import java.util.function.Supplier;

/**
 * Shows each champion where their ground-targeted abilities would land: a circle on the ground
 * ({@link GroundTarget}), in particles only that player sees, in the ability's key colour and sized
 * by the {@code aim} table of the datapack's stats chart.
 * The ability being aimed (pressed once, waiting for the confirming press) shows, with a second,
 * inner ring; {@code abilities.indicators: always} shows every other ready one too. Abilities that
 * can't be cast (on cooldown, not enough mana) show nothing.
 */
final class Indicators {
    private static final Map<Ability, Color> COLORS = Map.of(
            Ability.Q, Color.fromRGB(255, 216, 33),
            Ability.W, Color.fromRGB(90, 230, 110),
            Ability.E, Color.fromRGB(80, 170, 255),
            Ability.R, Color.fromRGB(255, 80, 80));
    /** Blocks between the particles of a circle. */
    private static final double SPACING = 0.35;

    private final Datapack datapack;
    private final Supplier<CombatSettings> settings;
    private final Function<Player, Ability> aiming;

    Indicators(Datapack datapack, Supplier<CombatSettings> settings, Function<Player, Ability> aiming) {
        this.datapack = datapack;
        this.settings = settings;
        this.aiming = aiming;
    }

    /** Every 2 ticks. */
    void tick() {
        CombatSettings config = settings.get();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!datapack.isChampion(player)) continue;
            Ability aimed = aiming.apply(player);
            boolean showReady = config.indicators() == CombatSettings.Indicators.ALWAYS;
            for (Ability ability : Ability.values()) {
                if (ability != aimed && !showReady) continue;
                int range = datapack.score(player, "aim." + ability.id(), 0);
                if (range <= 0 || !AbilityInputListener.castable(datapack, player, ability)) continue;
                Location target = GroundTarget.find(player, config, range / 10.0);
                if (target == null) continue;
                double radius = Math.max(0.3, datapack.score(player, "aim." + ability.id() + ".radius", 10) / 10.0);
                circle(player, target, radius, COLORS.get(ability));
                if (ability == aimed) circle(player, target, radius * 0.6, COLORS.get(ability));
            }
        }
    }

    private static void circle(Player player, Location center, double radius, Color color) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 0.9f);
        int points = Math.max(10, (int) Math.ceil(2 * Math.PI * radius / SPACING));
        for (int i = 0; i < points; i++) {
            double a = 2 * Math.PI * i / points;
            player.spawnParticle(Particle.DUST, center.getX() + Math.cos(a) * radius, center.getY() + 0.1,
                    center.getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0, dust);
        }
        player.spawnParticle(Particle.DUST, center.getX(), center.getY() + 0.1, center.getZ(), 1, 0, 0, 0, 0, dust);
    }
}
