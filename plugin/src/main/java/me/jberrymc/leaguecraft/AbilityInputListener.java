package me.jberrymc.leaguecraft;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.entity.Marker;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Turns Minecraft actions into LeagueCraft casts: hotbar keys 1-4 for Q/W/E/R, drop and F for summoner spells.
 *
 * <p>Every input only picks *what* to cast; the cast itself is the datapack's own function run as
 * the player, so cooldowns, mana, crowd control and the HUD behave exactly as the datapack defines.
 *
 * <p>While playing a champion the hand rests on hotbar key 8 (see {@link WeaponDisplay}). Hotbar
 * keys are buttons: the slot change is cancelled, so the client snaps back to 8 and pressing the
 * same key again casts again. The drop key and F never move items.
 *
 * <p>Left click (the basic attack) is handled by {@link BasicAttackListener}.
 *
 * <p>With /lc dev on, every input is echoed to the player's chat with the result (see {@link DevLog}).
 */
final class AbilityInputListener implements Listener {
    private final Datapack datapack;
    private final WeaponDisplay weapon;
    private final DevLog dev;
    private final Supplier<InputSettings> settings;
    private final Supplier<CombatSettings> combat;
    private final AimPoint aimPoint;
    private final Logger log;
    /** Last server tick each player triggered each action: one click can arrive as two events. */
    private final Map<UUID, Map<KeyAction, Long>> lastTick = new HashMap<>();
    /** Tag of the marker a ground-targeted ability reads its target from. */
    static final String GROUND_TARGET = "lc.ground_target";
    /** Tag of the marker a skill shot reads the crosshair's aim point from. */
    static final String AIM_TARGET = "lc.aim_target";

    /** A ground-targeted ability being aimed (first press seen, waiting for the confirming one). */
    private record Aiming(Ability ability) {}

    private final Map<UUID, Aiming> aiming = new HashMap<>();
    /** Whether Shift was down at the last input update, so a held Shift cancels once. */
    private final Map<UUID, Boolean> sneakDown = new HashMap<>();

    AbilityInputListener(Datapack datapack, WeaponDisplay weapon, DevLog dev,
                         Supplier<InputSettings> settings, Supplier<CombatSettings> combat, AimPoint aimPoint,
                         Logger log) {
        this.datapack = datapack;
        this.weapon = weapon;
        this.dev = dev;
        this.settings = settings;
        this.combat = combat;
        this.aimPoint = aimPoint;
        this.log = log;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHotbarKey(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        InputSettings config = settings.get();
        String input = "hotbar " + (event.getNewSlot() + 1) + " (from " + (event.getPreviousSlot() + 1) + ")";
        if (!ready(player, input, config)) return;
        event.setCancelled(true);
        KeyAction action = config.hotbar().get(event.getNewSlot());
        if (action == null) {
            dev.ignored(player, input, InputSettings.reserved(event.getNewSlot())
                    ? "key reserved (scroll wheel / rest slot)" : "not mapped in config");
            return;
        }
        perform(player, action, input, config);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        InputSettings config = settings.get();
        ItemStack dropped = event.getItemDrop().getItemStack();
        String input = "drop (" + dropped.getType().getKey().getKey() + ")";
        if (!ready(player, input, config)) return;
        if (!config.abilityItems().contains(dropped.getType()) && !weapon.isDisplay(dropped)) {
            dev.ignored(player, input, "not an ability item, dropped normally");
            return;
        }
        // Ability items and the weapon copy are never thrown; Paper puts the item back.
        event.setCancelled(true);
        // Only the drop key counts. A drop from the open inventory screen comes from another slot,
        // so the hand still holds something.
        if (!player.getInventory().getItemInMainHand().isEmpty()) {
            dev.ignored(player, input, "dropped from the inventory screen, not the drop key");
            return;
        }
        perform(player, config.drop(), input, config);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        InputSettings config = settings.get();
        if (!ready(player, "F (swap hands)", config)) return;
        // Cast directly instead of letting the datapack notice the missing offhand item a tick
        // later (it also ignores the first swap after a champion is picked).
        event.setCancelled(true);
        perform(player, config.swapHands(), "F (swap hands)", config);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastTick.remove(event.getPlayer().getUniqueId());
        aiming.remove(event.getPlayer().getUniqueId());
        sneakDown.remove(event.getPlayer().getUniqueId());
        dev.forget(event.getPlayer());
    }

    // ------------------------------------------------------------------ casting

    boolean canCast(Player player, InputSettings config) {
        return whyNot(player, config) == null;
    }

    /** Null if the player can cast; otherwise the reason, for dev mode. */
    private String whyNot(Player player, InputSettings config) {
        if (player.getGameMode() == GameMode.SPECTATOR) return "spectator";
        if (!player.getScoreboardTags().contains("champion")) return "not playing a champion (no 'champion' tag)";
        if (config.requireMatch() && !datapack.matchRunning()) {
            return "no match running (_ingame isn't 1; or set require-match: false)";
        }
        return null;
    }

    /** True if the player can cast; otherwise reports the reason in dev mode. */
    boolean ready(Player player, String input, InputSettings config) {
        String reason = whyNot(player, config);
        if (reason == null) return true;
        dev.ignored(player, input, reason);
        return false;
    }

    private void perform(Player player, KeyAction action, String input, InputSettings config) {
        if (action == null || action == KeyAction.NONE) {
            dev.ignored(player, input, "mapped to none");
            return;
        }
        long tick = Bukkit.getCurrentTick();
        Map<KeyAction, Long> last = lastTick.computeIfAbsent(player.getUniqueId(), id -> new EnumMap<>(KeyAction.class));
        if (Long.valueOf(tick).equals(last.put(action, tick))) {
            dev.ignored(player, input, "duplicate event this tick (" + action.id() + " already cast)");
            return;
        }

        // Ground-targeted abilities in confirm mode: the first press aims (Indicators shows where it
        // lands), a second press of the same key or a left click casts, Shift cancels. Abilities that
        // can't be cast (on cooldown, not enough mana) cast straight away, so the datapack can say so.
        Ability ability = action.ability();
        if (ability != null && combat.get().groundCast() == CombatSettings.GroundCast.CONFIRM
                && datapack.score(player, "aim." + ability.id(), 0) > 0 && castable(datapack, player, ability)) {
            Aiming current = aiming.get(player.getUniqueId());
            if (current == null || current.ability() != ability) {
                aiming.put(player.getUniqueId(), new Aiming(ability));
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.4f, 1.6f);
                dev.ignored(player, input, "aiming " + action.id() + ": press again or left click to cast, Shift to cancel");
                return;
            }
        }
        aiming.remove(player.getUniqueId());
        cast(player, action, input, config);
    }

    /** The ground-targeted ability this player is aiming, or null. */
    Ability aiming(Player player) {
        Aiming a = aiming.get(player.getUniqueId());
        return a == null ? null : a.ability();
    }

    /** A left click while aiming casts the aimed ability. True if it did (so no basic attack). */
    boolean confirmAim(Player player, String input) {
        Aiming a = aiming.remove(player.getUniqueId());
        if (a == null) return false;
        cast(player, KeyAction.of(a.ability()), input + " (confirms " + a.ability().id() + ")", settings.get());
        return true;
    }

    /** Off cooldown and enough mana (or energy) for it: the datapack's own checks. */
    static boolean castable(Datapack datapack, Player player, Ability ability) {
        return datapack.score(player, "cd_" + ability.id(), 0) <= 0
                && datapack.score(player, "mana", 0) >= datapack.score(player, "mana_" + ability.id(), 0);
    }

    @EventHandler
    public void onInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        boolean down = event.getInput().isSneak();
        boolean was = Boolean.TRUE.equals(sneakDown.put(player.getUniqueId(), down));
        if (down && !was && aiming.remove(player.getUniqueId()) != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, SoundCategory.MASTER, 0.4f, 0.8f);
            dev.ignored(player, "Shift", "aim cancelled");
        }
    }

    private void cast(Player player, KeyAction action, String input, InputSettings config) {
        String function = switch (action) {
            case SUMMONER_1 -> config.summonerOneFunction();
            case SUMMONER_2 -> config.summonerTwoFunction();
            default -> config.functionFor(action.ability());
        };
        // A ground-targeted ability (aim.<key> above 0) lands where the crosshair meets the ground:
        // a marker there for the datapack to read while the ability runs (lc.ground_target).
        Marker target = null;
        int aimRange = action.ability() == null ? 0 : datapack.score(player, "aim." + action.ability().id(), 0);
        if (aimRange > 0) {
            Location at = GroundTarget.find(player, combat.get(), aimRange / 10.0);
            if (at == null) {
                dev.ignored(player, input, "no ground to cast " + action.id() + " on within " + aimRange / 10.0 + " blocks");
                return;
            }
            target = at.getWorld().spawn(at, Marker.class, m -> {
                m.setPersistent(false);
                m.addScoreboardTag(GROUND_TARGET);
            });
        }
        // Skill shots fire at what the crosshair points at: a marker there (lc.aim_target).
        Marker aim = null;
        if (action.ability() != null) {
            aim = player.getWorld().spawn(aimPoint.find(player), Marker.class, m -> {
                m.setPersistent(false);
                m.addScoreboardTag(AIM_TARGET);
            });
        }
        boolean ok;
        try {
            ok = datapack.runAs(player, function);
        } finally {
            if (target != null) target.remove();
            if (aim != null) aim.remove();
        }
        if (action.ability() != null) {
            // use_<x> rebuilds the ability items and resets the HUD labels.
            weapon.apply(player);
            datapack.writeLabels(player, config.hudLabels());
        }
        dev.cast(player, input, action.id(), function, ok);
        if (config.debug()) {
            log.info(player.getName() + " " + input + " -> " + action.id() + " (" + function + ")" + (ok ? "" : " FAILED"));
        }
    }
}
