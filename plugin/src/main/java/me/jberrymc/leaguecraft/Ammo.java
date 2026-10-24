package me.jberrymc.leaguecraft;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
 * Ammo for champions' basic attacks, shown in a boss bar.
 *
 * <p>How much ammo and how long a reload takes are stats in the datapack's stats chart
 * (settings.mcb), which sets the {@code ammo.max} and {@code ammo.reload} (ticks) scores. A champion
 * with {@code ammo.max} 0 (melee) never runs out. A reload starts by itself when the last shot is
 * fired, or early with the reload key (sprint by default); it refills the whole magazine.
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
        final BossBar bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        boolean shown;
        /** Whether the reload key was down at the last input update, so a held key reloads once. */
        boolean keyDown;
    }

    Ammo(Datapack datapack, DevLog dev, Supplier<CombatSettings> settings) {
        this.datapack = datapack;
        this.dev = dev;
        this.settings = settings;
    }

    /** Called every few ticks for each champion: picks up stat changes and shows the bar if they have ammo. */
    void apply(Player player) {
        State s = sync(player);
        boolean show = s.max > 0;
        if (show && !s.shown) player.showBossBar(s.bar);
        if (!show && s.shown) player.hideBossBar(s.bar);
        s.shown = show;
        draw(s);
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
        draw(s);
        return true;
    }

    /** Every tick: finishes reloads. */
    void tick() {
        long now = Bukkit.getCurrentTick();
        for (Map.Entry<UUID, State> entry : states.entrySet()) {
            State s = entry.getValue();
            if (s.reloadEnd < 0) continue;
            if (now >= s.reloadEnd) {
                s.reloadEnd = -1;
                s.left = s.max;
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) dev.ammo(player, "reloaded", s.left, s.max);
            }
            draw(s);
        }
    }

    /** Hides the bar and forgets the player once they aren't playing a champion. */
    void remove(Player player) {
        State s = states.remove(player.getUniqueId());
        if (s != null && s.shown) player.hideBossBar(s.bar);
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
        draw(s);
    }

    private static void draw(State s) {
        if (s.max <= 0) return;
        if (s.reloadEnd >= 0) {
            long left = Math.max(0, s.reloadEnd - Bukkit.getCurrentTick());
            s.bar.name(Component.text("Reloading", NamedTextColor.YELLOW));
            s.bar.progress(Math.clamp(1f - (float) left / s.reloadTicks, 0f, 1f));
            s.bar.color(BossBar.Color.YELLOW);
        } else {
            s.bar.name(Component.text("Ammo  " + s.left + " / " + s.max, s.left == 0 ? NamedTextColor.RED : NamedTextColor.WHITE));
            s.bar.progress(Math.clamp((float) s.left / s.max, 0f, 1f));
            s.bar.color(BossBar.Color.WHITE);
        }
    }
}
