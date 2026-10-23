package me.jberrymc.leaguecraft;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

/**
 * Keeps a champion's hand on the rest slot (hotbar key 8) with a copy of their weapon in it.
 *
 * <p>The datapack shows the weapon through the ability items in slots 1-4 (their model is the
 * champion's weapon). The copy has the same model but none of the ability data, so the datapack
 * never treats what you're holding as an ability, and the scroll wheel can only reach keys 7 and 9.
 */
final class WeaponDisplay {
    private final NamespacedKey marker;

    WeaponDisplay(Plugin plugin) {
        this.marker = new NamespacedKey(plugin, "weapon_display");
    }

    /** Puts the hand on the rest slot and makes sure the copy there matches slot 1. */
    void apply(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack source = inventory.getItem(0);
        if (source == null || source.isEmpty()) return;   // ability items not there yet (picking, respawning)

        CustomModelData model = source.getData(DataComponentTypes.CUSTOM_MODEL_DATA);
        ItemStack current = inventory.getItem(InputSettings.REST_SLOT);
        if (!isDisplay(current) || !Objects.equals(model, current.getData(DataComponentTypes.CUSTOM_MODEL_DATA))
                || current.getType() != source.getType()) {
            inventory.setItem(InputSettings.REST_SLOT, copyOf(source, model));
        }
        if (inventory.getHeldItemSlot() != InputSettings.REST_SLOT) {
            inventory.setHeldItemSlot(InputSettings.REST_SLOT);
        }
    }

    /** Removes the copy once the player isn't playing a champion any more. */
    void remove(Player player) {
        if (isDisplay(player.getInventory().getItem(InputSettings.REST_SLOT))) {
            player.getInventory().setItem(InputSettings.REST_SLOT, null);
        }
    }

    boolean isDisplay(ItemStack item) {
        return item != null && !item.isEmpty()
                && item.getPersistentDataContainer().has(marker, PersistentDataType.BYTE);
    }

    private ItemStack copyOf(ItemStack source, CustomModelData model) {
        ItemStack copy = ItemStack.of(source.getType());
        if (model != null) copy.setData(DataComponentTypes.CUSTOM_MODEL_DATA, model);
        copy.setData(DataComponentTypes.CUSTOM_NAME, Component.text(" "));
        copy.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        copy.editPersistentDataContainer(pdc -> pdc.set(marker, PersistentDataType.BYTE, (byte) 1));
        return copy;
    }
}
