package me.jberrymc.leaguecraft;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatSettingsTest {
    private static CombatSettings withZoom(double melee, double ranged, Map<String, Double> champions) {
        return new CombatSettings(true, 3.0, 0.8, 0.25, true,
                true, melee, ranged, champions,
                true, true, 100.0, CombatSettings.ReloadKey.SPRINT);
    }

    @Test
    void meleeAndRangedTagsPickTheirZoom() {
        CombatSettings settings = withZoom(0.15, 0.3, Map.of());
        assertEquals(new CombatSettings.Zoom(0.15, "melee"), settings.zoomFor(Set.of("champion", "garen", "melee")));
        assertEquals(new CombatSettings.Zoom(0.3, "ranged"), settings.zoomFor(Set.of("champion", "ezreal", "ranged")));
    }

    @Test
    void championEntryWinsOverMeleeOrRanged() {
        CombatSettings settings = withZoom(0.15, 0.3, Map.of("jhin", 0.5));
        assertEquals(new CombatSettings.Zoom(0.5, "jhin"), settings.zoomFor(Set.of("champion", "jhin", "ranged")));
        assertEquals(new CombatSettings.Zoom(0.3, "ranged"), settings.zoomFor(Set.of("champion", "ashe", "ranged")));
    }

    @Test
    void untaggedPlayersCountAsRanged() {
        assertEquals(0.3, withZoom(0.15, 0.3, Map.of()).zoomFor(Set.of()).slow());
    }

    @Test
    void slowdownMustBeBetweenZeroAndOne() {
        assertTrue(CombatSettings.isSlowdown(0.3));
        assertFalse(CombatSettings.isSlowdown(0));
        assertFalse(CombatSettings.isSlowdown(1));
        assertFalse(CombatSettings.isSlowdown(2.2));   // an old camera distance
    }
}
