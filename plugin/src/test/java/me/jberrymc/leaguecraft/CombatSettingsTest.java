package me.jberrymc.leaguecraft;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombatSettingsTest {
    private static CombatSettings withZoom(double melee, double ranged, Map<String, Double> champions) {
        return new CombatSettings(true, 3.0, 0.8, 0.25, true,
                true, melee, ranged, champions,
                true, true, 3.5, 48.0, 10);
    }

    @Test
    void meleeAndRangedTagsPickTheirZoom() {
        CombatSettings settings = withZoom(2.2, 1.2, Map.of());
        assertEquals(new CombatSettings.Zoom(2.2, "melee"), settings.zoomFor(Set.of("champion", "garen", "melee")));
        assertEquals(new CombatSettings.Zoom(1.2, "ranged"), settings.zoomFor(Set.of("champion", "ezreal", "ranged")));
    }

    @Test
    void championEntryWinsOverMeleeOrRanged() {
        CombatSettings settings = withZoom(2.2, 1.2, Map.of("jhin", 0.8));
        assertEquals(new CombatSettings.Zoom(0.8, "jhin"), settings.zoomFor(Set.of("champion", "jhin", "ranged")));
        assertEquals(new CombatSettings.Zoom(1.2, "ranged"), settings.zoomFor(Set.of("champion", "ashe", "ranged")));
    }

    @Test
    void untaggedPlayersCountAsRanged() {
        assertEquals(1.2, withZoom(2.2, 1.2, Map.of()).zoomFor(Set.of()).distance());
    }
}
