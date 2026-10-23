package me.jberrymc.leaguecraft;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** config.yml, read once per load/reload. */
record InputSettings(
        boolean requireMatch,
        Set<Material> abilityItems,
        KeyAction rightClick,
        KeyAction sneak,
        KeyAction drop,
        KeyAction swapHands,
        Map<Integer, KeyAction> hotbar,
        Map<String, String> hudLabels,
        String castFunction,
        String summonerOneFunction,
        String summonerTwoFunction,
        boolean firstTimeTip,
        boolean debug) {

    /**
     * Hotbar slot (0-based) the hand rests on while playing a champion: key 8. Scrolling can only
     * move one slot either way, to keys 7 or 9, so those three keys can never cast.
     */
    static final int REST_SLOT = 7;

    static boolean reserved(int slot) {
        return slot >= REST_SLOT - 1 && slot <= REST_SLOT + 1;
    }

    static InputSettings from(FileConfiguration config, Logger log) {
        Set<Material> items = EnumSet.noneOf(Material.class);
        for (String id : config.getStringList("ability-items")) {
            Material material = Material.matchMaterial(id);
            if (material == null) {
                log.warning("Unknown item in ability-items: " + id);
            } else {
                items.add(material);
            }
        }

        // keys.hotbar-<n>: hotbar key number (1-9) -> action
        Map<Integer, KeyAction> hotbar = new HashMap<>();
        ConfigurationSection keys = config.getConfigurationSection("keys");
        if (keys != null) {
            for (String key : keys.getKeys(false)) {
                if (!key.startsWith("hotbar-")) continue;
                int slot;
                try {
                    slot = Integer.parseInt(key.substring("hotbar-".length())) - 1;
                } catch (NumberFormatException e) {
                    slot = -1;
                }
                KeyAction action = KeyAction.parse(keys.getString(key));
                if (slot < 0 || slot > 8 || action == null) {
                    log.warning("Ignoring keys." + key + ": " + keys.getString(key));
                } else if (reserved(slot) && action != KeyAction.NONE) {
                    log.warning("Ignoring keys." + key + ": hotbar keys 7, 8 and 9 are reserved "
                            + "(the hand rests on 8, and the scroll wheel reaches 7 and 9)");
                } else if (action != KeyAction.NONE) {
                    hotbar.put(slot, action);
                }
            }
        }

        // HUD storage slot -> label. q/w/e/r sit under the ability icons, d under summoner spell 1.
        Map<String, String> labels = new LinkedHashMap<>();
        ConfigurationSection labelSection = config.getConfigurationSection("hud-labels");
        if (labelSection != null) {
            for (Ability ability : Ability.values()) {
                String label = labelSection.getString(ability.id());
                if (label != null && !label.isEmpty()) labels.put(ability.id(), label);
            }
            String summoner = labelSection.getString("summoner-1");
            if (summoner != null && !summoner.isEmpty()) labels.put("d", summoner);
        }

        String function = config.getString("cast-function", "_champion:abilities/use_%s");
        if (!function.contains("%s")) {
            log.warning("cast-function has no %s, so every key would cast the same function: " + function);
        }
        return new InputSettings(
                config.getBoolean("require-match", true),
                items,
                action(config, "keys.right-click", KeyAction.Q, log),
                action(config, "keys.sneak", KeyAction.W, log),
                action(config, "keys.drop", KeyAction.SUMMONER_1, log),
                action(config, "keys.swap-hands", KeyAction.SUMMONER_2, log),
                Map.copyOf(hotbar),
                Map.copyOf(labels),
                function,
                config.getString("summoner-1-function", "_champion:spell/check"),
                config.getString("summoner-2-function", "_champion:spell/check_2"),
                config.getBoolean("first-time-tip", true),
                config.getBoolean("debug", false));
    }

    String functionFor(Ability ability) {
        return castFunction.formatted(ability.id());
    }

    private static KeyAction action(FileConfiguration config, String path, KeyAction fallback, Logger log) {
        String raw = config.getString(path);
        if (raw == null) return fallback;
        KeyAction action = KeyAction.parse(raw);
        if (action == null) {
            log.warning("Unknown action for " + path + ": " + raw + " (use q, w, e, r, summoner-1, summoner-2 or none)");
            return fallback;
        }
        return action;
    }
}
