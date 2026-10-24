package me.jberrymc.leaguecraft;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
    private final Logger log;
    /** Last server tick each player triggered each action: one click can arrive as two events. */
    private final Map<UUID, Map<KeyAction, Long>> lastTick = new HashMap<>();

    AbilityInputListener(Datapack datapack, WeaponDisplay weapon, DevLog dev,
                         Supplier<InputSettings> settings, Logger log) {
        this.datapack = datapack;
        this.weapon = weapon;
        this.dev = dev;
        this.settings = settings;
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

        String function = switch (action) {
            case SUMMONER_1 -> config.summonerOneFunction();
            case SUMMONER_2 -> config.summonerTwoFunction();
            default -> config.functionFor(action.ability());
        };
        boolean ok = datapack.runAs(player, function);
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
