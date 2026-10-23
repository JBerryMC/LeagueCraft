#ifndef TPSCAM_CROSSHAIR_GLSL
#define TPSCAM_CROSSHAIR_GLSL

// ============================================================
//  Shoulder Cam - crosshair settings
//  Java hides the crosshair in F5. Any GUI text glyph drawn in the
//  marker colour below is moved to the exact centre of the screen,
//  so a datapack or plugin can show a crosshair through the action bar.
// ============================================================

// Marker colour #4E5C24 = rgb(78, 92, 36). Must match the text colour
// used in the action bar command.
#define TPS_CROSSHAIR_MARKER vec3(78.0, 92.0, 36.0)

// Crosshair size in GUI pixels (crosshair.png is 13x13, so 13 = 1:1).
// Keep it a whole number so the texture stays sharp.
#define TPS_CROSSHAIR_SIZE 13.0

// ------------------------------------------------------------

bool tps_is_crosshair(mat4 proj, vec4 color) {
    bool isGui = abs(proj[2][3]) < 0.5;
    vec3 diff = abs(color.rgb * 255.0 - TPS_CROSSHAIR_MARKER);
    return isGui && max(diff.r, max(diff.g, diff.b)) < 1.5;
}

// Returns the clip-space position of this glyph corner, snapped to a square
// in the middle of the screen on whole GUI pixels. The corner is built in GUI
// space and sent through the GUI projection, so it stays correct on both the
// OpenGL and Vulkan backends. Glyph quads are emitted top-left, bottom-left,
// bottom-right, top-right.
vec4 tps_crosshair_position(mat4 proj, float viewZ, int vertexIndex) {
    // GUI ortho maps 0..guiWidth and 0..guiHeight (y down) onto the screen
    vec2 guiSize = vec2(2.0 / abs(proj[0][0]), 2.0 / abs(proj[1][1]));

    vec2 minGui = floor((guiSize - vec2(TPS_CROSSHAIR_SIZE)) * 0.5);
    vec2 maxGui = minGui + vec2(TPS_CROSSHAIR_SIZE);

    int corner = vertexIndex % 4;
    float gx = (corner == 0 || corner == 1) ? minGui.x : maxGui.x;
    float gy = (corner == 0 || corner == 3) ? minGui.y : maxGui.y;

    return proj * vec4(gx, gy, viewZ, 1.0);
}

#endif
