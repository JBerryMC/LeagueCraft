package me.jberrymc.leaguecraft;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Dev mode: echoes every input the plugin sees to that player's chat, with what happened.
 * Toggle per player with /lc dev. Not saved; it's off again after a restart.
 */
final class DevLog {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final Set<UUID> enabled = new HashSet<>();

    boolean toggle(Player player) {
        if (!enabled.remove(player.getUniqueId())) {
            enabled.add(player.getUniqueId());
            return true;
        }
        return false;
    }

    boolean on(Player player) {
        return enabled.contains(player.getUniqueId());
    }

    void forget(Player player) {
        enabled.remove(player.getUniqueId());
    }

    /** Something was cast (or the datapack command failed). */
    void cast(Player player, String input, String action, String function, boolean ok) {
        if (!on(player)) return;
        send(player, "<dark_gray>[dev]</dark_gray> <white><input></white> <gray>→</gray> <gold><action></gold> "
                        + (ok ? "<green>✔ ran <function>" : "<red>✘ command failed: <function>"),
                input, action, function);
    }

    /** The input arrived but nothing was cast; {@code reason} says why. */
    void ignored(Player player, String input, String reason) {
        if (!on(player)) return;
        send(player, "<dark_gray>[dev]</dark_gray> <white><input></white> <gray>→ ignored: <reason>",
                input, "", reason);
    }

    /** A basic attack hitscan: where the crosshair ray started, what it aimed at, and what the shot hit. */
    void shot(Player player, String input, Hitscan.Shot shot) {
        if (!on(player)) return;
        String result = switch (shot.kind()) {
            case ENTITY -> shot.entity().getType().getKey().getKey();
            case BLOCK -> "block " + shot.block().getType().getKey().getKey();
            case MISS -> "nothing";
        };
        send(player, "<dark_gray>[dev]</dark_gray> <white><input></white> <gray>→</gray> <gold><action></gold> "
                        + "<gray><reason>",
                input, "hitscan " + result,
                String.format(Locale.ROOT, "%.1fm of %.1f · aim from %s to %s", shot.distance(), shot.range(),
                        format(shot.aimOrigin()), format(shot.aimPoint())));
    }

    /** The animation a champion's model should be playing changed (see {@link MovementState}). */
    void animation(Player player, String from, String to, String detail) {
        if (!on(player)) return;
        send(player, "<dark_gray>[dev]</dark_gray> <light_purple>animation</light_purple> <gray><input> → "
                        + "</gray><gold><action></gold> <dark_gray>(<reason>)",
                from, to, detail);
    }

    /** Zoom started (with the camera distance and where it came from) or ended. */
    void zoom(Player player, boolean in, double distance, String why) {
        if (!on(player)) return;
        send(player, "<dark_gray>[dev]</dark_gray> <aqua><input></aqua> <gray>→ camera <action> <dark_gray>(<reason>)",
                in ? "zoom in" : "zoom out", String.format(Locale.ROOT, "%.1f", distance), why);
    }

    private static String format(Vec3 v) {
        return String.format(Locale.ROOT, "%.2f %.2f %.2f", v.x(), v.y(), v.z());
    }

    private static void send(Player player, String template, String input, String action, String other) {
        player.sendMessage(MM.deserialize(template,
                Placeholder.unparsed("input", input),
                Placeholder.unparsed("action", action),
                Placeholder.unparsed("function", other),
                Placeholder.unparsed("reason", other)));
    }
}
