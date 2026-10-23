#ifndef TPSCAM_CAMERA_GLSL
#define TPSCAM_CAMERA_GLSL

// ============================================================
//  Shoulder Cam - camera settings (Minecraft Java 26.3)
//  Edit these numbers, then press F3+T in game to reload.
//  Units are blocks, measured from the vanilla F5 camera position.
// ============================================================

// Sideways shift. Positive = camera moves right (player appears left of the
// crosshair), negative = left shoulder. 0.0 = vanilla centred view.
#define TPS_SHOULDER 0.8

// Vertical shift. Positive = camera moves up.
#define TPS_HEIGHT 0.25

// Pull the camera closer (positive) or push it back (negative).
// Leave this at 0.0 and set the distance with the vanilla attribute instead:
//   /attribute @s minecraft:camera_distance base set 3
// (the included datapack does this). The attribute keeps vanilla's wall
// collision working; this shader value doesn't know about walls.
#define TPS_FORWARD 0.0

// Set to 0 to switch the camera offset off without removing the pack.
#define TPS_ENABLED 1

// ------------------------------------------------------------

// Perspective matrices are the 3D world; orthographic ones are the GUI.
bool tps_is_world(mat4 proj) {
    return abs(proj[2][3]) > 0.5;
}

// Takes a view-space position (ModelViewMat * position) and projects it,
// shifting the camera first if this is world geometry.
vec4 tps_project(mat4 proj, vec4 viewPos) {
#if TPS_ENABLED
    if (tps_is_world(proj)) {
        viewPos.xyz -= vec3(TPS_SHOULDER, TPS_HEIGHT, -TPS_FORWARD);
    }
#endif
    return proj * viewPos;
}

#endif
