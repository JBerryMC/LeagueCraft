package me.jberrymc.leaguecraft;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The over-the-shoulder view for champions, together with the ShoulderCam shaders in the resource
 * pack. The shaders move the F5 camera beside the player; this sets how far back it sits and
 * draws the crosshair, which vanilla hides in F5.
 *
 * <p>The crosshair is a glyph in ShoulderCam's marker colour, which the text shader moves to the
 * middle of the screen. Every champion HUD ends with {@code lol:main crosshair.<uuid>}, so the
 * glyph rides along in the HUD's action bar instead of fighting it for the action bar.
 */
final class ThirdPerson implements Listener {
    /**
     * The crosshair, as SNBT text. The glyph is 13 px wide (14 px advance), so 14 px of negative
     * space after it keeps the centred action bar from shifting. The empty first element keeps
     * the space from inheriting the crosshair's font.
     */
    static final String CROSSHAIR = "[\"\",{text:\"\uE000\",font:\"tpscam:crosshair\",color:\"#4E5C24\"},"
            + "{translate:\"space.-14\"}]";

    private final Datapack datapack;
    private final Attribute cameraDistance;
    /** Players whose camera distance this changed, so it's only reset for them. */
    private final Set<UUID> applied = new HashSet<>();

    ThirdPerson(Datapack datapack, Logger log) {
        this.datapack = datapack;
        this.cameraDistance = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)
                .get(Key.key("minecraft", "camera_distance"));
        if (cameraDistance == null) {
            log.warning("This server has no minecraft:camera_distance attribute; third-person.camera-distance is ignored.");
        }
    }

    /** Called every few ticks for each champion. {@code refreshCrosshair} rewrites the HUD storage. */
    void apply(Player player, CombatSettings settings, boolean refreshCrosshair) {
        if (!settings.thirdPerson()) {
            remove(player);
            return;
        }
        AttributeInstance distance = cameraDistance == null ? null : player.getAttribute(cameraDistance);
        if (distance != null && distance.getBaseValue() != settings.cameraDistance()) {
            distance.setBaseValue(settings.cameraDistance());
        }
        applied.add(player.getUniqueId());
        if (refreshCrosshair) {
            if (settings.crosshair()) {
                datapack.writeCrosshair(player, CROSSHAIR);
            } else {
                datapack.removeCrosshair(player);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }

    /** Puts the camera back to vanilla once the player isn't playing a champion. */
    void remove(Player player) {
        if (!applied.remove(player.getUniqueId())) return;
        AttributeInstance distance = cameraDistance == null ? null : player.getAttribute(cameraDistance);
        if (distance != null) distance.setBaseValue(distance.getDefaultValue());
        datapack.removeCrosshair(player);
    }
}
