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

const float DIM_ALPHA = 0.35;

int stateAt(ivec3 cell) {
    if (any(lessThan(cell, ivec3(0))) || any(greaterThanEqual(cell, ivec3(128)))) return 0;
    int index = cell.x + 128 * (cell.z + 128 * cell.y);
    return int(round(texelFetch(FogSampler, ivec2(index % 2048, index / 2048), 0).r * 255.0));
}

float sightAt(ivec3 cell) {
    return stateAt(cell) == 2 ? 1.0 : 0.0;
}

// Interpolate lighting around cell centres, not the exploration state. Unknown
// terrain outside the radius stays opaque; only the dim/clear boundary softens.
float smoothSight(vec3 grid) {
    vec3 centred = grid - 0.5;
    ivec3 base = ivec3(floor(centred));
    vec3 t = smoothstep(vec3(0.0), vec3(1.0), fract(centred));
    float low = mix(mix(sightAt(base), sightAt(base + ivec3(1,0,0)), t.x),
                    mix(sightAt(base + ivec3(0,0,1)), sightAt(base + ivec3(1,0,1)), t.x), t.z);
    float high = mix(mix(sightAt(base + ivec3(0,1,0)), sightAt(base + ivec3(1,1,0)), t.x),
                     mix(sightAt(base + ivec3(0,1,1)), sightAt(base + ivec3(1,1,1)), t.x), t.z);
    return mix(low, high, t.y);
}

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
    // The circle guarantees terrain is at least remembered, not necessarily
    // in sight. Occlusion must dim nearby ground, never turn it opaque black.
    vec2 fromPlayer = world.xz - PlayerPosition.xz;
    bool nearby = dot(fromPlayer, fromPlayer) <= TerrainRange * TerrainRange;
    vec3 grid = world / 2.0 - GridOrigin;
    int state = stateAt(ivec3(floor(grid)));
    if (nearby) fragColor = vec4(0.0, 0.0, 0.0, DIM_ALPHA * (1.0 - smoothSight(grid)));
    else if (state != 0) fragColor = vec4(0.0, 0.0, 0.0, DIM_ALPHA);
    else fragColor = vec4(0.035, 0.045, 0.06, 1.0);
}
