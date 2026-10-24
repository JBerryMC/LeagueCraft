package me.jberrymc.leaguecraft;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The datapack's inventory menus (queue, champion select, settings): a click on a menu item runs it.
 *
 * <p>The datapack lays the menu out as items in the player's inventory and runs an entry when its
 * slot is empty ({@code lc:menu/main}: "unless data entity @s Inventory[{Slot:12b}]"), which relied
 * on the player picking the item up. Instead, a click on a menu slot is cancelled (nothing ends up
 * on the cursor or dropped), the item is removed, and {@code lc:menu/main} runs straight away, so
 * the datapack sees the empty slot and runs that entry as it always did, then lays the menu out
 * again.
 */
final class MenuListener implements Listener {
    /** The datapack's menu entries are in the main inventory, slots 9-35. */
    private static final int FIRST_MENU_SLOT = 9;
    private static final int LAST_MENU_SLOT = 35;

    private final Datapack datapack;

    MenuListener(Datapack datapack) {
        this.datapack = datapack;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !inMenu(player)) return;
        if (event.getClickedInventory() != player.getInventory()) return;
        int slot = event.getSlot();
        if (slot < FIRST_MENU_SLOT || slot > LAST_MENU_SLOT) return;
        event.setCancelled(true);
        ItemStack item = player.getInventory().getItem(slot);
        if (item == null || item.isEmpty()) return;
        player.getInventory().setItem(slot, null);
        datapack.runAs(player, "lc:menu/main");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && inMenu(player)) event.setCancelled(true);
    }

    private static boolean inMenu(Player player) {
        return player.getScoreboardTags().contains("see_settings");
    }
}
