package me.jberrymc.leaguecraft;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
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

    /** A melee basic attack: what it hit in its cone. */
    void melee(Player player, String input, List<Entity> hits, double range) {
        if (!on(player)) return;
        StringJoiner names = new StringJoiner(", ");
        for (Entity hit : hits) names.add(hit.getType().getKey().getKey());
        send(player, "<dark_gray>[dev]</dark_gray> <white><input></white> <gray>→</gray> <gold><action></gold> "
                        + "<gray><reason>",
                input, "melee hit " + hits.size(), (hits.isEmpty() ? "nothing" : names.toString())
                        + String.format(Locale.ROOT, " · range %.1f", range));
    }

    /** Ammo changed: a reload started or finished. */
    void ammo(Player player, String what, int left, int max) {
        if (!on(player)) return;
        send(player, "<dark_gray>[dev]</dark_gray> <yellow>ammo</yellow> <gray>→ <action> <dark_gray>(<reason>)",
                "", what, left + "/" + max);
    }

    /** The animation a champion's model should be playing changed (see {@link MovementState}). */
    void animation(Player player, String from, String to, String detail) {
        if (!on(player)) return;
        send(player, "<dark_gray>[dev]</dark_gray> <light_purple>animation</light_purple> <gray><input> → "
                        + "</gray><gold><action></gold> <dark_gray>(<reason>)",
                from, to, detail);
    }

    /** Zoom started (how much narrower the view gets, any slowdown, and where it came from) or ended. */
    void zoom(Player player, boolean in, double view, double slow, String why) {
        if (!on(player)) return;
        String effect = in
                ? String.format(Locale.ROOT, "view %.0f%% narrower", view * 100)
                        + (slow > 0 ? String.format(Locale.ROOT, ", %.0f%% slower", slow * 100) : "")
                : "normal view";
        send(player, "<dark_gray>[dev]</dark_gray> <aqua><input></aqua> <gray>→ <action> <dark_gray>(<reason>)",
                in ? "zoom in" : "zoom out", effect, why);
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
