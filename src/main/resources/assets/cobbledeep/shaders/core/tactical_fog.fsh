#version 150
uniform sampler2D DepthSampler;
uniform sampler2D FogSampler;
uniform mat4 InverseProjection;
uniform mat4 InverseView;
uniform vec3 CameraPosition;
uniform vec3 GridOrigin;
uniform vec3 PlayerPosition;
uniform float TerrainRange;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    // Conceal the horizon too, so distant silhouettes do not map unknown terrain.
    if (depth >= 0.9999999) {
        fragColor = vec4(0.035, 0.045, 0.06, 1.0);
        return;
    }
    vec4 view = InverseProjection * vec4(texCoord * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec3 relative = (InverseView * vec4(view.xyz / view.w, 0.0)).xyz;
    // Bias into the surface so integer block boundaries don't sample air on
    // the camera side of an unexplored wall.
    vec3 world = CameraPosition + relative + normalize(relative) * 0.02;
    // Terrain uses a horizontal circle at every elevation. Check before the
    // discovery atlas so newly visible ground never waits for a sync packet.
    vec2 fromPlayer = world.xz - PlayerPosition.xz;
    if (dot(fromPlayer, fromPlayer) <= TerrainRange * TerrainRange) {
        fragColor = vec4(0.0);
        return;
    }
    ivec3 cell = ivec3(floor(world / 2.0) - GridOrigin);
    if (any(lessThan(cell, ivec3(0))) || any(greaterThanEqual(cell, ivec3(128)))) {
        fragColor = vec4(0.035, 0.045, 0.06, 1.0);
        return;
    }
    int index = cell.x + 128 * (cell.z + 128 * cell.y);
    int state = int(round(texelFetch(FogSampler, ivec2(index % 2048, index / 2048), 0).r * 255.0));
    if (state == 0) fragColor = vec4(0.035, 0.045, 0.06, 1.0);
    else if (state == 1) fragColor = vec4(0.0, 0.0, 0.0, 0.60);
    else fragColor = vec4(0.0);
}
