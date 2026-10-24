package me.jberrymc.leaguecraft;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * The over-the-shoulder view for champions, together with the ShoulderCam shaders in the resource
 * pack. The shaders move the F5 camera beside the player; this sets how far back it sits, zooms it
 * in while right click is held, and draws the crosshair, which vanilla hides in F5.
 *
 * <p>The crosshair is a glyph in ShoulderCam's marker colour, which the text shader moves to the
 * middle of the screen. Every champion HUD ends with {@code lol:main crosshair.<uuid>}, so the
 * glyph rides along in the HUD's action bar instead of fighting it for the action bar.
 *
 * <p><b>Zoom.</b> A server can't change the field of view, but it sets the camera distance, so
 * zooming pulls the camera in (like aiming down sights in a third-person shooter). The crosshair
 * ray doesn't depend on camera distance, so the hitscan aims the same zoomed or not. Holding right
 * click makes the client send a click every 4 ticks: the first one sets the zoomed distance, and
 * {@link #ZOOM_RELEASE_TICKS} ticks after the last one the normal distance is set back. That's
 * one attribute change each way; nothing is sent while the zoom is held.
 *
 * <p>Everything here goes through the API (no commands) except the crosshair, which lives in
 * datapack storage: it's written once when someone becomes a champion and removed afterwards.
 */
final class ThirdPerson implements Listener {
    /**
     * The crosshair, as SNBT text. The glyph is 13 px wide (14 px advance), so 14 px of negative
     * space after it keeps the centred action bar from shifting. The empty first element keeps
     * the space from inheriting the crosshair's font.
     */
    static final String CROSSHAIR = "[\"\",{text:\"\",font:\"tpscam:crosshair\",color:\"#4E5C24\"},"
            + "{translate:\"space.-14\"}]";
    /** Held right click repeats every 4 ticks; a gap longer than this means it was let go. */
    static final int ZOOM_RELEASE_TICKS = 6;

    private final Datapack datapack;
    private final DevLog dev;
    private final Supplier<CombatSettings> settings;
    private final Attribute cameraDistance;
    /** Champions this applies to right now; only their camera is changed and reset. */
    private final Set<UUID> applied = new HashSet<>();
    /** Champions whose crosshair is in the HUD storage. */
    private final Set<UUID> crosshairShown = new HashSet<>();
    /** Server tick of each zooming player's last right click. */
    private final Map<UUID, Integer> zoomClick = new HashMap<>();

    ThirdPerson(Datapack datapack, DevLog dev, Supplier<CombatSettings> settings, Logger log) {
        this.datapack = datapack;
        this.dev = dev;
        this.settings = settings;
        this.cameraDistance = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE)
                .get(Key.key("minecraft", "camera_distance"));
        if (cameraDistance == null) {
            log.warning("This server has no minecraft:camera_distance attribute; camera distance and zoom are ignored.");
        }
    }

    /**
     * Called every few ticks for each champion. Only changes something when it's out of date: the
     * first call for a champion, after /lc reload, or if something else moved the camera.
     */
    void apply(Player player, CombatSettings config) {
        if (!config.thirdPerson()) {
            remove(player);
            return;
        }
        UUID id = player.getUniqueId();
        applied.add(id);
        if (!zoomClick.containsKey(id)) setDistance(player, config.cameraDistance());
        if (config.crosshair() && crosshairShown.add(id)) {
            datapack.writeCrosshair(player, CROSSHAIR);
        } else if (!config.crosshair() && crosshairShown.remove(id)) {
            datapack.removeCrosshair(player);
        }
    }

    /** Every tick: ends zooms whose right click was let go. */
    void tick() {
        if (zoomClick.isEmpty()) return;
        int now = Bukkit.getCurrentTick();
        CombatSettings config = settings.get();
        zoomClick.entrySet().removeIf(entry -> {
            if (now - entry.getValue() <= ZOOM_RELEASE_TICKS) return false;
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                setDistance(player, config.cameraDistance());
                dev.zoom(player, false, config.cameraDistance(), "right click let go");
            }
            return true;
        });
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onRightClick(PlayerInteractEvent event) {
        // Right-clicking air arrives already cancelled (nothing to use), so no ignoreCancelled here.
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (zoom(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onRightClickEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (zoom(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }

    /** Puts the camera back to vanilla once the player isn't playing a champion. */
    void remove(Player player) {
        UUID id = player.getUniqueId();
        zoomClick.remove(id);
        if (crosshairShown.remove(id)) datapack.removeCrosshair(player);
        if (!applied.remove(id)) return;
        AttributeInstance distance = cameraDistance == null ? null : player.getAttribute(cameraDistance);
        if (distance != null) setDistance(player, distance.getDefaultValue());
    }

    /** A right click from a champion: starts or holds the zoom. Returns false if it isn't theirs to zoom. */
    private boolean zoom(Player player) {
        CombatSettings config = settings.get();
        if (!config.zoom() || !applied.contains(player.getUniqueId())) return false;
        if (zoomClick.put(player.getUniqueId(), Bukkit.getCurrentTick()) == null) {
            CombatSettings.Zoom zoom = config.zoomFor(player.getScoreboardTags());
            setDistance(player, zoom.distance());
            dev.zoom(player, true, zoom.distance(), zoom.source());
        }
        return true;
    }

    /** Sets the camera distance if it isn't that already. */
    private void setDistance(Player player, double value) {
        AttributeInstance distance = cameraDistance == null ? null : player.getAttribute(cameraDistance);
        if (distance != null && distance.getBaseValue() != value) distance.setBaseValue(value);
    }
}
