package me.jberrymc.leaguecraft;

import java.util.Locale;

/**
 * Which animation a champion's model should be playing, worked out from how the player is moving.
 * Pure logic (no server types) so it can be unit tested; {@link RigTest} feeds it each tick.
 */
enum MovementState {
    IDLE, WALK, SPRINT, SNEAK, SNEAK_WALK, JUMP, FALL;

    /** Horizontal speed (blocks per tick) above which the player counts as moving. Walking is ~0.21. */
    static final double MOVING = 0.03;

    /**
     * @param onGround   whether the player is standing on something
     * @param dy         change in height since last tick
     * @param horizontal horizontal distance moved since last tick
     * @param sneaking   holding shift
     * @param sprinting  sprinting (the client decides; it needs sprint held while moving forward)
     */
    static MovementState classify(boolean onGround, double dy, double horizontal, boolean sneaking, boolean sprinting) {
        if (!onGround) return dy > 0 ? JUMP : FALL;
        boolean moving = horizontal > MOVING;
        if (sneaking) return moving ? SNEAK_WALK : SNEAK;
        if (!moving) return IDLE;
        return sprinting ? SPRINT : WALK;
    }

    /** Name as an animation id: idle, walk, sprint, sneak, sneak_walk, jump, fall. */
    String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
