package me.jberrymc.leaguecraft;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShoulderCameraTest {
    private static final double EPS = 1e-9;

    @Test
    void forwardUsesMinecraftAngles() {
        assertVec(new Vec3(0, 0, 1), ShoulderCamera.forward(0, 0));    // yaw 0 faces south (+Z)
        assertVec(new Vec3(-1, 0, 0), ShoulderCamera.forward(90, 0));  // yaw 90 faces west (-X)
        assertVec(new Vec3(0, -1, 0), ShoulderCamera.forward(0, 90));  // pitch 90 looks down
    }

    @Test
    void rightIsTheScreensRightHandSide() {
        assertVec(new Vec3(-1, 0, 0), ShoulderCamera.right(0));   // facing south, west is on the right
        assertVec(new Vec3(0, 0, -1), ShoulderCamera.right(90));  // facing west, north is on the right
    }

    @Test
    void upTiltsWithPitch() {
        assertVec(new Vec3(0, 1, 0), ShoulderCamera.up(0, 0));
        double h = Math.sqrt(0.5);
        // Looking 45° down while facing south, the top of the screen leans south.
        assertVec(new Vec3(0, h, h), ShoulderCamera.up(0, 45));
    }

    @Test
    void axesAreOrthonormalAtAnyAngle() {
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            float yaw = random.nextFloat() * 360 - 180;
            float pitch = random.nextFloat() * 180 - 90;
            Vec3 f = ShoulderCamera.forward(yaw, pitch);
            Vec3 r = ShoulderCamera.right(yaw);
            Vec3 u = ShoulderCamera.up(yaw, pitch);
            assertEquals(1, f.length(), EPS);
            assertEquals(1, r.length(), EPS);
            assertEquals(1, u.length(), EPS);
            assertEquals(0, f.dot(r), EPS);
            assertEquals(0, f.dot(u), EPS);
            assertEquals(0, r.dot(u), EPS);
        }
    }

    /**
     * What the shader does: camera at the F5 position (distance d behind the eye), then everything
     * shifted by (shoulder, height) in camera space. Whatever d is, the point in the middle of the
     * screen at depth t must lie on the ray from aimOrigin along forward.
     */
    @Test
    void crosshairRayPassesThroughAimOriginForAnyCameraDistance() {
        Vec3 eye = new Vec3(10.5, 65.62, -3.25);
        float yaw = 37.5f;
        float pitch = -20f;
        double shoulder = 0.8;
        double height = 0.25;
        Vec3 f = ShoulderCamera.forward(yaw, pitch);
        Vec3 r = ShoulderCamera.right(yaw);
        Vec3 u = ShoulderCamera.up(yaw, pitch);
        Vec3 origin = ShoulderCamera.aimOrigin(eye, yaw, pitch, shoulder, height);

        for (double d : new double[] {0.5, 3, 4, 12}) {
            Vec3 camera = eye.subtract(f.multiply(d)).add(r.multiply(shoulder)).add(u.multiply(height));
            for (double t : new double[] {d, d + 5, d + 40}) {
                Vec3 centre = camera.add(f.multiply(t));
                Vec3 fromOrigin = centre.subtract(origin);
                // No sideways or vertical component left: it's on the aim ray.
                assertEquals(0, fromOrigin.dot(r), 1e-9);
                assertEquals(0, fromOrigin.dot(u), 1e-9);
            }
        }
    }

    @Test
    void aimOriginIsBesideTheEye() {
        Vec3 eye = new Vec3(0, 64, 0);
        // Facing south: 0.8 to the right is west (-X), 0.25 up.
        assertVec(new Vec3(-0.8, 64.25, 0), ShoulderCamera.aimOrigin(eye, 0, 0, 0.8, 0.25));
    }

    private static void assertVec(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x(), actual.x(), EPS, "x of " + actual);
        assertEquals(expected.y(), actual.y(), EPS, "y of " + actual);
        assertEquals(expected.z(), actual.z(), EPS, "z of " + actual);
    }
}
