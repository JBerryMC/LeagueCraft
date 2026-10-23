package me.jberrymc.leaguecraft;

/**
 * Where the ShoulderCam crosshair points, worked out on the server.
 *
 * <p>The ShoulderCam resource pack keeps the vanilla F5 camera and then shifts the whole picture in
 * camera space: {@code TPS_SHOULDER} blocks to the right and {@code TPS_HEIGHT} blocks up (see
 * {@code assets/tpscam/shaders/include/camera.glsl}). A shift doesn't rotate anything, so the ray
 * through the middle of the screen is parallel to where the player looks and passes beside their
 * eye at that same offset. The camera's distance (and vanilla pulling it in at walls) only moves
 * the camera along that ray, so it never changes what the crosshair points at.
 *
 * <p>Uses Minecraft's angles: yaw 0 faces +Z (south), yaw 90 faces -X (west), pitch 90 looks down.
 */
final class ShoulderCamera {
    private ShoulderCamera() {}

    /** Unit vector the player is looking along (same as Bukkit's {@code Location#getDirection}). */
    static Vec3 forward(float yawDegrees, float pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double horizontal = Math.cos(pitch);
        return new Vec3(-Math.sin(yaw) * horizontal, -Math.sin(pitch), Math.cos(yaw) * horizontal);
    }

    /** Unit vector to the right of the screen. Always level: Minecraft's camera never rolls. */
    static Vec3 right(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
    }

    /** Unit vector to the top of the screen; tilts with pitch. */
    static Vec3 up(float yawDegrees, float pitchDegrees) {
        return right(yawDegrees).cross(forward(yawDegrees, pitchDegrees));
    }

    /**
     * The point beside the eye that the crosshair ray passes through. Starting the aim trace here
     * (instead of at the camera) skips anything between the camera and the player.
     */
    static Vec3 aimOrigin(Vec3 eye, float yawDegrees, float pitchDegrees, double shoulder, double height) {
        return eye.add(right(yawDegrees).multiply(shoulder)).add(up(yawDegrees, pitchDegrees).multiply(height));
    }
}
