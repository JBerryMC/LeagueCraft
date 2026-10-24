package me.jberrymc.leaguecraft;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Everything the plugin knows about the LeagueCraft datapack: its scoreboard state and how to run
 * one of its functions as a player.
 */
final class Datapack {
    /** Objective the datapack creates in lc:core/setup; used to tell whether the pack is installed. */
    private static final String MARKER_OBJECTIVE = "use_ability";

    private final CommandSender silentSender;

    Datapack(Logger log, Supplier<Boolean> debug) {
        // Runs commands like the console, but its feedback ("Running function ...") goes here
        // instead of flooding the log on every cast.
        this.silentSender = Bukkit.createCommandSender(message -> {
            if (debug.get()) log.info("[datapack] " + PlainTextComponentSerializer.plainText().serialize(message));
        });
    }

    boolean installed() {
        return scoreboard().getObjective(MARKER_OBJECTIVE) != null;
    }

    /** A match is running: score _ingame in objective "game" is 1. */
    boolean matchRunning() {
        Objective game = scoreboard().getObjective("game");
        if (game == null) return false;
        Score ingame = game.getScore("_ingame");
        return ingame.isScoreSet() && ingame.getScore() == 1;
    }

    /** The datapack tags a player "champion" once they've picked one. */
    boolean isChampion(Player player) {
        return player.getScoreboardTags().contains("champion")
                && player.getGameMode() != GameMode.SPECTATOR;
    }

    /** {@code execute as <player> at @s run function <function>}. Returns false if the command failed. */
    boolean runAs(Player player, String function) {
        return Bukkit.dispatchCommand(silentSender,
                "execute as " + player.getUniqueId() + " at @s run function " + function);
    }

    /** Runs a command like the console does (quietly). Returns false if it failed. */
    boolean command(String command) {
        return Bukkit.dispatchCommand(silentSender, command);
    }

    /** A score of a fake player (like waittime in objective queue), or {@code fallback}. */
    int score(String holder, String objective, int fallback) {
        Objective o = scoreboard().getObjective(objective);
        if (o == null) return fallback;
        Score score = o.getScore(holder);
        return score.isScoreSet() ? score.getScore() : fallback;
    }

    /** An entity's score, or {@code fallback} if the objective or the score isn't there. */
    int score(Entity entity, String objective, int fallback) {
        Objective o = scoreboard().getObjective(objective);
        if (o == null) return fallback;
        Score score = o.getScore(entity instanceof Player player ? player.getName() : entity.getUniqueId().toString());
        return score.isScoreSet() ? score.getScore() : fallback;
    }

    /** Sets an entity's score, if the datapack made the objective and it isn't that already. */
    void setScore(Entity entity, String objective, int value) {
        Objective o = scoreboard().getObjective(objective);
        if (o == null) return;
        Score score = o.getScore(entity instanceof Player player ? player.getName() : entity.getUniqueId().toString());
        if (!score.isScoreSet() || score.getScore() != value) score.setScore(value);
    }

    /** The attack sound (and Aatrox's attack animation), once per basic attack. */
    void attack(Player attacker) {
        runAs(attacker, "lc:champion/aa/plugin.attack");
    }

    /**
     * Deals the attacker's basic attack to one target through the datapack's own damage system,
     * with every on-hit effect. The datapack finds the target by the {@code temp_dmg} tag.
     */
    boolean hit(Player attacker, Entity target) {
        target.addScoreboardTag("temp_dmg");
        try {
            return Bukkit.dispatchCommand(silentSender, "execute as " + attacker.getUniqueId() + " at "
                    + target.getUniqueId() + " run function lc:champion/aa/plugin.hit");
        } finally {
            target.removeScoreboardTag("temp_dmg");
        }
    }

    /**
     * Overwrites the key labels under each ability icon. The HUD reads them from
     * {@code lol:main keybind.<q|w|e|r>.<uuid ints>} as text components.
     * A label starting with {@code key.} becomes a keybind component, which shows whatever key the
     * player bound to that control; anything else is shown as written.
     */
    void writeLabels(Player player, Map<String, String> labels) {
        if (labels.isEmpty()) return;
        String path = uuidPath(player.getUniqueId());
        for (Map.Entry<String, String> label : labels.entrySet()) {
            String value = label.getValue();
            String content = value.startsWith("key.") ? "keybind:" + quote(value) : "text:" + quote(value);
            // q/w/e/r inherit the HUD's label style. The summoner label (d) sits in the icon font,
            // so it carries the same style as the F label next to it.
            String style = label.getKey().equals("d") ? SUMMONER_LABEL_STYLE : "";
            Bukkit.dispatchCommand(silentSender, "data modify storage lol:main keybind." + label.getKey()
                    + "." + path + " set value [{" + content + style + "}]");
        }
    }

    /** Sets {@code lol:main crosshair.<uuid ints>}, which every champion HUD shows at its end. */
    void writeCrosshair(Player player, String snbtText) {
        Bukkit.dispatchCommand(silentSender, "data modify storage lol:main crosshair."
                + uuidPath(player.getUniqueId()) + " set value " + snbtText);
    }

    void removeCrosshair(Player player) {
        Bukkit.dispatchCommand(silentSender, "data remove storage lol:main crosshair." + uuidPath(player.getUniqueId()));
    }

    private static final String SUMMONER_LABEL_STYLE =
            ",font:\"minecraft:custom/keybind_small\",bold:true,color:\"#ffd821\"";

    /** The four UUID ints, dot-separated, the way the datapack builds its per-player storage paths. */
    static String uuidPath(UUID uuid) {
        long most = uuid.getMostSignificantBits();
        long least = uuid.getLeastSignificantBits();
        return (int) (most >> 32) + "." + (int) most + "." + (int) (least >> 32) + "." + (int) least;
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static Scoreboard scoreboard() {
        return Bukkit.getScoreboardManager().getMainScoreboard();
    }
}
