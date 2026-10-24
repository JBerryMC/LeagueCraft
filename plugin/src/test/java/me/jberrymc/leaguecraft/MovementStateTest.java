package me.jberrymc.leaguecraft;

import org.junit.jupiter.api.Test;

import static me.jberrymc.leaguecraft.MovementState.FALL;
import static me.jberrymc.leaguecraft.MovementState.IDLE;
import static me.jberrymc.leaguecraft.MovementState.JUMP;
import static me.jberrymc.leaguecraft.MovementState.SNEAK;
import static me.jberrymc.leaguecraft.MovementState.SNEAK_WALK;
import static me.jberrymc.leaguecraft.MovementState.SPRINT;
import static me.jberrymc.leaguecraft.MovementState.WALK;
import static me.jberrymc.leaguecraft.MovementState.classify;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementStateTest {
    @Test
    void onTheGround() {
        assertEquals(IDLE, classify(true, 0, 0, false, false));
        assertEquals(IDLE, classify(true, 0, 0.01, false, false));   // tiny drift isn't walking
        assertEquals(WALK, classify(true, 0, 0.21, false, false));
        assertEquals(SPRINT, classify(true, 0, 0.28, false, true));
        assertEquals(IDLE, classify(true, 0, 0, false, true));        // sprint flag lingers when stopped
    }

    @Test
    void sneakingWinsOverSprinting() {
        assertEquals(SNEAK, classify(true, 0, 0, true, false));
        assertEquals(SNEAK_WALK, classify(true, 0, 0.06, true, true));
    }

    @Test
    void inTheAir() {
        assertEquals(JUMP, classify(false, 0.42, 0.2, false, true));
        assertEquals(FALL, classify(false, -0.1, 0, false, false));
        assertEquals(FALL, classify(false, 0, 0, true, false));       // apex counts as falling
    }
}
