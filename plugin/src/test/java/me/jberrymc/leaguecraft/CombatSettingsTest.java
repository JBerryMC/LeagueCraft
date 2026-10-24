package me.jberrymc.leaguecraft;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatSettingsTest {
    private static CombatSettings withZoom(double melee, double ranged, Map<String, Double> champions) {
        return withZoom(melee, ranged, champions, Map.of());
    }

    private static CombatSettings withZoom(double melee, double ranged, Map<String, Double> champions,
                                           Map<String, Double> slowChampions) {
        return new CombatSettings(true, 3.0, 0.8, 0.25, true,
                true, melee, ranged, champions, 0, 0.2, slowChampions,
                true, true, 100.0, CombatSettings.ReloadKey.SPRINT, CombatSettings.Indicators.AIMED,
                CombatSettings.GroundCast.CONFIRM);
    }

    @Test
    void meleeAndRangedTagsPickTheirZoom() {
        CombatSettings settings = withZoom(0.15, 0.3, Map.of());
        assertEquals(new CombatSettings.Zoom(0.15, 0, "melee"), settings.zoomFor(Set.of("champion", "garen", "melee")));
        assertEquals(new CombatSettings.Zoom(0.3, 0.2, "ranged"), settings.zoomFor(Set.of("champion", "ezreal", "ranged")));
    }

    @Test
    void championEntryWinsOverMeleeOrRanged() {
        CombatSettings settings = withZoom(0.15, 0.3, Map.of("jhin", 0.49), Map.of("jhin", 0.4));
        assertEquals(new CombatSettings.Zoom(0.49, 0.4, "jhin"), settings.zoomFor(Set.of("champion", "jhin", "ranged")));
        assertEquals(new CombatSettings.Zoom(0.3, 0.2, "ranged"), settings.zoomFor(Set.of("champion", "ashe", "ranged")));
    }

    @Test
    void aChampionCanOverrideJustTheSlowdown() {
        CombatSettings settings = withZoom(0.15, 0.3, Map.of(), Map.of("garen", 0.1));
        assertEquals(new CombatSettings.Zoom(0.15, 0.1, "garen"), settings.zoomFor(Set.of("champion", "garen", "melee")));
    }

    @Test
    void untaggedPlayersCountAsRanged() {
        assertEquals(0.3, withZoom(0.15, 0.3, Map.of()).zoomFor(Set.of()).view());
    }

    @Test
    void zoomIsAboveZeroAndAtMostTheMaximum() {
        assertTrue(CombatSettings.isZoom(0.3));
        assertTrue(CombatSettings.isZoom(CombatSettings.MAX_ZOOM));
        assertFalse(CombatSettings.isZoom(0));
        assertFalse(CombatSettings.isZoom(0.5));   // the client can't narrow the view that far
    }
}
