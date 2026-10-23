package me.jberrymc.leaguecraft;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
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
    /** Objective the datapack creates in _lol:setup; used to tell whether the pack is installed. */
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
