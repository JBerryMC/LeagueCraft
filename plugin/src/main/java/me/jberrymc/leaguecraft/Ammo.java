package me.jberrymc.leaguecraft;

import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Ammo for champions' basic attacks.
 *
 * <p>How much ammo and how long a reload takes are stats in the datapack's stats chart
 * (settings.mcb), which sets the {@code ammo.max} and {@code ammo.reload} (ticks) scores. A champion
 * with {@code ammo.max} 0 (melee) never runs out. A reload starts by itself when the last shot is
 * fired, or early with the reload key (sprint by default); it refills the whole magazine.
 *
 * <p>For the HUD, the plugin keeps two scores up to date: {@code ammo.left} (shots left) and
 * {@code ammo.reloading} (ticks until the reload ends, 0 when not reloading).
 */
final class Ammo implements Listener {
    private static final int FALLBACK_RELOAD_TICKS = 30;

    private final Datapack datapack;
    private final DevLog dev;
    private final Supplier<CombatSettings> settings;
    private final Map<UUID, State> states = new HashMap<>();

    private static final class State {
        int max;
        int left;
        int reloadTicks;
        /** Server tick the current reload ends, or -1 when not reloading. */
        long reloadEnd = -1;
        /** Whether the reload key was down at the last input update, so a held key reloads once. */
        boolean keyDown;
    }

    Ammo(Datapack datapack, DevLog dev, Supplier<CombatSettings> settings) {
        this.datapack = datapack;
        this.dev = dev;
        this.settings = settings;
    }

    /** Called every few ticks for each champion: picks up stat changes. */
    void apply(Player player) {
        publish(player, sync(player));
    }

    /** Takes one shot. False (and nothing is fired) while reloading; an empty magazine starts a reload. */
    boolean take(Player player, String input) {
        State s = sync(player);
        if (s.max <= 0) return true;
        if (s.reloadEnd >= 0) {
            dev.ignored(player, input, "reloading (" + (s.reloadEnd - Bukkit.getCurrentTick()) + " ticks left)");
            return false;
        }
        if (s.left <= 0) {
            startReload(player, s, "out of ammo");
            dev.ignored(player, input, "out of ammo, reloading");
            return false;
        }
        s.left--;
        if (s.left == 0) startReload(player, s, "magazine empty");
        publish(player, s);
        return true;
    }

    /** Every tick: counts reloads down and finishes them. */
    void tick() {
        long now = Bukkit.getCurrentTick();
        for (Map.Entry<UUID, State> entry : states.entrySet()) {
            State s = entry.getValue();
            if (s.reloadEnd < 0) continue;
            Player player = Bukkit.getPlayer(entry.getKey());
            if (now >= s.reloadEnd) {
                s.reloadEnd = -1;
                s.left = s.max;
                if (player != null) dev.ammo(player, "reloaded", s.left, s.max);
            }
            if (player != null) publish(player, s);
        }
    }

    /** Forgets the player once they aren't playing a champion. */
    void remove(Player player) {
        states.remove(player.getUniqueId());
    }

    @EventHandler
    public void onInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        State s = states.get(player.getUniqueId());
        if (s == null) return;
        boolean down = pressed(settings.get().reloadKey(), event.getInput());
        boolean justPressed = down && !s.keyDown;
        s.keyDown = down;
        if (justPressed && s.max > 0 && s.reloadEnd < 0 && s.left < s.max) startReload(player, s, "reload key");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    private static boolean pressed(CombatSettings.ReloadKey key, Input input) {
        return switch (key) {
            case SPRINT -> input.isSprint();
            case SNEAK -> input.isSneak();
            case NONE -> false;
        };
    }

    /** The player's state, refilled if their ammo stats changed (a new champion, or the stats chart after /reload). */
    private State sync(Player player) {
        State s = states.computeIfAbsent(player.getUniqueId(), id -> new State());
        int max = Math.max(0, datapack.score(player, "ammo.max", 0));
        s.reloadTicks = Math.max(1, datapack.score(player, "ammo.reload", FALLBACK_RELOAD_TICKS));
        if (max != s.max) {
            s.max = max;
            s.left = max;
            s.reloadEnd = -1;
        }
        return s;
    }

    private void startReload(Player player, State s, String why) {
        s.reloadEnd = Bukkit.getCurrentTick() + s.reloadTicks;
        dev.ammo(player, "reloading (" + why + ", " + s.reloadTicks + " ticks)", s.left, s.max);
        publish(player, s);
    }

    /** Writes ammo.left and ammo.reloading for the HUD. */
    private void publish(Player player, State s) {
        datapack.setScore(player, "ammo.left", s.left);
        datapack.setScore(player, "ammo.reloading", s.reloadEnd < 0 ? 0 : (int) Math.max(0, s.reloadEnd - Bukkit.getCurrentTick()));
    }
}
