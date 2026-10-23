package me.jberrymc.leaguecraft;

/** Minimal immutable 3D vector, so the camera math has no server types in it and can be unit tested. */
record Vec3(double x, double y, double z) {
    Vec3 add(Vec3 o) {
        return new Vec3(x + o.x, y + o.y, z + o.z);
    }

    Vec3 subtract(Vec3 o) {
        return new Vec3(x - o.x, y - o.y, z - o.z);
    }

    Vec3 multiply(double s) {
        return new Vec3(x * s, y * s, z * s);
    }

    double dot(Vec3 o) {
        return x * o.x + y * o.y + z * o.z;
    }

    Vec3 cross(Vec3 o) {
        return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x);
    }

    double length() {
        return Math.sqrt(dot(this));
    }

    /** Unit vector in the same direction; {@code fallback} if this is (almost) zero. */
    Vec3 normalize(Vec3 fallback) {
        double length = length();
        return length < 1e-9 ? fallback : multiply(1 / length);
    }
}
