package me.jberrymc.leaguecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.plugin.Plugin;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * An Animated Java rig's bones in their rest pose, read from {@code rigs/<name>.json} in the plugin
 * jar. Generate that file with {@code plugin/tools/extract_aj_rig.py <name>}.
 */
record Rig(String name, List<Bone> bones) {
    /**
     * One bone: an item display showing {@code item} with custom model data {@code model}, placed by
     * {@code transformation} relative to the rig's root (the player's feet).
     */
    record Bone(String name, Material item, float model, ItemDisplay.ItemDisplayTransform display,
                Matrix4f transformation) {}

    static Rig load(Plugin plugin, String name) throws IOException {
        String path = "rigs/" + name + ".json";
        try (InputStream in = plugin.getResource(path)) {
            if (in == null) throw new IOException("missing " + path + " in the plugin jar");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            List<Bone> bones = new ArrayList<>();
            for (JsonElement element : root.getAsJsonArray("bones")) {
                JsonObject bone = element.getAsJsonObject();
                Material item = Material.matchMaterial(bone.get("item").getAsString());
                if (item == null) throw new IOException(path + ": unknown item " + bone.get("item"));
                JsonArray values = bone.getAsJsonArray("transformation");
                float[] matrix = new float[16];
                for (int i = 0; i < 16; i++) matrix[i] = values.get(i).getAsFloat();
                bones.add(new Bone(
                        bone.get("name").getAsString(),
                        item,
                        bone.get("model").getAsFloat(),
                        ItemDisplay.ItemDisplayTransform.valueOf(bone.get("display").getAsString().toUpperCase(Locale.ROOT)),
                        // Minecraft writes transformations row by row; JOML reads column by column.
                        new Matrix4f().set(matrix).transpose()));
            }
            return new Rig(root.get("rig").getAsString(), List.copyOf(bones));
        }
    }
}
