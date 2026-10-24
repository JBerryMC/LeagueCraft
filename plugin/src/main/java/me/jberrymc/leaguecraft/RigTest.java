package me.jberrymc.leaguecraft;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import org.bukkit.Bukkit;
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * /lc rigtest: mounts a champion rig on the player, to try models that ride players instead of
 * players riding a mount. The vanilla /ride command refuses to put anything on a player, but the
 * API doesn't, and each client then moves the passengers with its own view of the player, so the
 * model follows without lag.
 *
 * <p>Only the rest pose is shown; the model doesn't animate yet. Each tick it works out which
 * animation <i>would</i> play ({@link MovementState}) and, in dev mode, reports every change.
 *
 * <ul>
 *   <li>Passengers sit at the top of the player's hitbox, so every bone is moved down by the
 *       player's current height (lower while sneaking).</li>
 *   <li>Passengers don't turn with their vehicle: the bones' yaw follows the player's yaw, and
 *       {@code teleport_duration} smooths it on the client.</li>
 *   <li>A teleport, death or world change can drop the passengers; they're put back next tick.</li>
 * </ul>
 */
final class RigTest implements Listener {
    private final Rig rig;
    private final DevLog dev;
    private final Map<UUID, Mounted> mounted = new HashMap<>();

    private static final class Mounted {
        final List<ItemDisplay> bones = new ArrayList<>();
        boolean wasInvisible;
        float yaw;
        double height;
        Location last;
        MovementState state = MovementState.IDLE;
    }

    RigTest(Rig rig, DevLog dev) {
        this.rig = rig;
        this.dev = dev;
    }

    String rigName() {
        return rig.name();
    }

    /** True while this player wears the test rig. */
    boolean has(Player player) {
        return mounted.containsKey(player.getUniqueId());
    }

    /** Mounts the rig, or takes it off if it's already on. Returns true if it's now on. */
    boolean toggle(Player player) {
        if (remove(player)) return false;
        Mounted m = new Mounted();
        m.wasInvisible = player.isInvisible();
        mounted.put(player.getUniqueId(), m);
        spawn(player, m);
        // Hide the player's own body so the model can be seen. Armor and held items still show.
        player.setInvisible(true);
        return true;
    }

    /** Takes the rig off. Returns false if the player didn't have one. */
    boolean remove(Player player) {
        Mounted m = mounted.remove(player.getUniqueId());
        if (m == null) return false;
        despawn(m);
        player.setInvisible(m.wasInvisible);
        return true;
    }

    void removeAll() {
        for (UUID id : List.copyOf(mounted.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                remove(player);
            } else {
                despawn(mounted.remove(id));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer());
    }

    /** Every tick: keep the rig on the player, facing their way, and report animation changes. */
    void tick() {
        for (Map.Entry<UUID, Mounted> entry : mounted.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null) continue;
            Mounted m = entry.getValue();

            if (m.bones.stream().anyMatch(bone -> !bone.isValid() || bone.getWorld() != player.getWorld())) {
                despawn(m);
                spawn(player, m);
            }
            for (ItemDisplay bone : m.bones) {
                if (bone.getVehicle() != player) player.addPassenger(bone);
            }

            double height = player.getHeight();
            if (height != m.height) {
                m.height = height;
                for (int i = 0; i < m.bones.size(); i++) {
                    ItemDisplay bone = m.bones.get(i);
                    bone.setInterpolationDelay(0);
                    bone.setTransformationMatrix(pose(rig.bones().get(i), height));
                }
            }

            float yaw = player.getLocation().getYaw();
            if (Math.abs(yaw - m.yaw) > 0.5f) {
                m.yaw = yaw;
                for (ItemDisplay bone : m.bones) bone.setRotation(yaw, 0);
            }

            Location now = player.getLocation();
            if (m.last != null && m.last.getWorld() == now.getWorld()) {
                double dy = now.getY() - m.last.getY();
                double horizontal = Math.hypot(now.getX() - m.last.getX(), now.getZ() - m.last.getZ());
                @SuppressWarnings("deprecation") // client-reported, which is exactly what animation wants
                boolean onGround = player.isOnGround();
                MovementState state = MovementState.classify(onGround, dy, horizontal,
                        player.isSneaking(), player.isSprinting());
                if (state != m.state) {
                    dev.animation(player, m.state.id(), state.id(), keys(player.getCurrentInput()));
                    m.state = state;
                }
            }
            m.last = now;
        }
    }

    private void spawn(Player player, Mounted m) {
        m.height = player.getHeight();
        m.yaw = player.getLocation().getYaw();
        Location at = player.getLocation();
        at.setPitch(0);
        for (Rig.Bone bone : rig.bones()) {
            ItemDisplay display = player.getWorld().spawn(at, ItemDisplay.class, d -> {
                d.setPersistent(false);
                ItemStack item = ItemStack.of(bone.item());
                item.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
                        CustomModelData.customModelData().addFloat(bone.model()).build());
                d.setItemStack(item);
                d.setItemDisplayTransform(bone.display());
                d.setTransformationMatrix(pose(bone, m.height));
                d.setInterpolationDuration(2);
                d.setTeleportDuration(1);
            });
            player.addPassenger(display);
            m.bones.add(display);
        }
    }

    private static void despawn(Mounted m) {
        for (ItemDisplay bone : m.bones) bone.remove();
        m.bones.clear();
    }

    /** The bone's rest pose, moved from the top of the player's hitbox down to their feet. */
    private static Matrix4f pose(Rig.Bone bone, double playerHeight) {
        return new Matrix4f().translation(0, (float) -playerHeight, 0).mul(bone.transformation());
    }

    private static String keys(Input input) {
        StringJoiner keys = new StringJoiner(" ");
        if (input.isForward()) keys.add("W");
        if (input.isLeft()) keys.add("A");
        if (input.isBackward()) keys.add("S");
        if (input.isRight()) keys.add("D");
        if (input.isJump()) keys.add("jump");
        if (input.isSneak()) keys.add("shift");
        if (input.isSprint()) keys.add("sprint");
        return keys.length() == 0 ? "no keys" : "keys: " + keys;
    }
}
