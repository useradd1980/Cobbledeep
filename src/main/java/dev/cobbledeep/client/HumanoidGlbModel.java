package dev.cobbledeep.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;

/**
 * First-pass, client-only glTF 2.0 skinned-mesh reader for Cobbledeep's Blender
 * humanoid preview. Reads a binary GLB, one skinned triangle primitive, one
 * four-influence skin, a LINEAR/STEP skeletal Action, and the five rigid sword
 * meshes attached to the baked TwoHandedWeapon_ctrl. Blender constraints must
 * be baked by glTF export; no IK calculations run inside Minecraft.
 *
 * This is deliberately separate from RatMeshModel (rigid child meshes).
 */
final class HumanoidGlbModel {
    private static final Logger LOGGER = LogUtils.getLogger();
    // Aesthetic calibration only: apply scale around the actual exported handle
    // centre, never around sword1's distant object origin or a world origin.
    private static final float SWORD_SCALE = readCalibration(
            "COBBLEDEEP_HUMANOID_SWORD_SCALE", 1.30f, 0.5f, 2f);
    // The optional offsets are measured in glTF world units in the opening
    // frame, BEFORE TwoHandedWeapon_ctrl's relative animated movement.
    private static final Vector3f SWORD_OFFSET = new Vector3f(
            readCalibration("COBBLEDEEP_HUMANOID_SWORD_OFFSET_X", 0f, -20f, 20f),
            readCalibration("COBBLEDEEP_HUMANOID_SWORD_OFFSET_Y", 0f, -20f, 20f),
            readCalibration("COBBLEDEEP_HUMANOID_SWORD_OFFSET_Z", 0f, -20f, 20f));
    private static final int GLB_MAGIC = 0x46546c67; // ASCII glTF, little endian
    private static final int JSON_CHUNK = 0x4e4f534a;
    private static final int BIN_CHUNK = 0x004e4942;
    private static final float[] ZERO3 = {0, 0, 0};
    private static final float[] ONE3 = {1, 1, 1};
    private static final float[] IDENTITY_ROTATION = {0, 0, 0, 1};

    private final Node[] nodes;
    private final int[] jointNodes;
    private final Matrix4f[] inverseBind;
    private final float[][] positions;
    private final float[][] normals;
    private final float[][] uvs;
    private final int[][] vertexJoints;
    private final float[][] vertexWeights;
    private final int[] triangleIndices;
    // The Blender export retains the five unskinned sword meshes even though
    // its glTF scene deliberately only exposes the humanoid. Keep the weapon
    // separate from skinning: it follows the animated two-hand weapon control.
    private final SwordPart[] swordParts;
    private final Vector3f swordPivot;
    private final int weaponControlNode;
    private final Matrix4f inverseWeaponAtStart;
    private final Curve[][] curves;
    private final float startTime;
    private final float endTime;

    private record Node(int parent, float[] translation, float[] rotation, float[] scale) {}
    private record Curve(float[] times, float[][] values, boolean step) {}
    private record SwordPart(float[][] positions, float[][] normals, float[][] uvs,
                             int[] indices, int tint) {}

    private HumanoidGlbModel(Node[] nodes, int[] jointNodes, Matrix4f[] inverseBind,
                             float[][] positions, float[][] normals, float[][] uvs,
                             int[][] vertexJoints, float[][] vertexWeights,
                             int[] triangleIndices, SwordPart[] swordParts,
                             Vector3f swordPivot, int weaponControlNode, Curve[][] curves,
                             float startTime, float endTime) {
        this.nodes = nodes;
        this.jointNodes = jointNodes;
        this.inverseBind = inverseBind;
        this.positions = positions;
        this.normals = normals;
        this.uvs = uvs;
        this.vertexJoints = vertexJoints;
        this.vertexWeights = vertexWeights;
        this.triangleIndices = triangleIndices;
        this.swordParts = swordParts;
        this.swordPivot = swordPivot == null ? new Vector3f() : new Vector3f(swordPivot);
        this.weaponControlNode = weaponControlNode;
        this.curves = curves;
        this.startTime = startTime;
        this.endTime = endTime;
        this.inverseWeaponAtStart = swordParts.length == 0 ? null :
                new Matrix4f(worldMatrix(weaponControlNode, startTime,
                        new Matrix4f[nodes.length])).invert();
    }

    static HumanoidGlbModel load(ResourceManager resources, ResourceLocation location) {
        try (var stream = resources.open(location)) {
            byte[] raw = stream.readAllBytes();
            ByteBuffer file = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
            if (file.getInt() != GLB_MAGIC || file.getInt() != 2 || file.getInt() != raw.length) {
                throw new IllegalArgumentException("Invalid GLB header: " + location);
            }
            JsonObject document = null;
            ByteBuffer binary = null;
            while (file.remaining() >= 8) {
                int size = file.getInt();
                int type = file.getInt();
                if (size < 0 || size > file.remaining()) throw new IllegalArgumentException("Bad GLB chunk size");
                byte[] chunk = new byte[size];
                file.get(chunk);
                if (type == JSON_CHUNK) {
                    document = JsonParser.parseString(new String(chunk, StandardCharsets.UTF_8).trim())
                            .getAsJsonObject();
                } else if (type == BIN_CHUNK) {
                    binary = ByteBuffer.wrap(chunk).order(ByteOrder.LITTLE_ENDIAN);
                }
            }
            if (document == null || binary == null) throw new IllegalArgumentException("Missing GLB JSON or BIN");
            HumanoidGlbModel model = decode(document, binary);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(raw);
            LOGGER.info("Cobbledeep preview GLB SHA-256={}, sword scale={}, handle pivot={}, offset={}",
                    HexFormat.of().formatHex(hash), SWORD_SCALE, model.swordPivot, SWORD_OFFSET);
            return model;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load Cobbledeep humanoid GLB: " + location, e);
        }
    }

    private static HumanoidGlbModel decode(JsonObject gltf, ByteBuffer binary) {
        Accessors reader = new Accessors(gltf, binary);
        JsonArray sourceNodes = gltf.getAsJsonArray("nodes");
        Node[] nodes = new Node[sourceNodes.size()];
        int[] parents = new int[nodes.length];
        Arrays.fill(parents, -1);
        for (int n = 0; n < sourceNodes.size(); n++) {
            JsonObject node = sourceNodes.get(n).getAsJsonObject();
            if (node.has("matrix")) throw new IllegalArgumentException("GLB matrix nodes not yet supported");
            if (node.has("children")) for (JsonElement child : node.getAsJsonArray("children")) {
                parents[child.getAsInt()] = n;
            }
        }
        for (int n = 0; n < nodes.length; n++) {
            JsonObject node = sourceNodes.get(n).getAsJsonObject();
            nodes[n] = new Node(parents[n], floats(node, "translation", ZERO3),
                    floats(node, "rotation", IDENTITY_ROTATION), floats(node, "scale", ONE3));
        }

        // Pick the actual skinned humanoid, not extra Cube/sword objects still
        // present in some Sketchfab-derived GLB exports.
        int skinIndex = -1;
        JsonObject primitive = null;
        for (JsonElement rawNode : sourceNodes) {
            JsonObject node = rawNode.getAsJsonObject();
            if (!node.has("mesh") || !node.has("skin")) continue;
            JsonObject mesh = gltf.getAsJsonArray("meshes").get(node.get("mesh").getAsInt()).getAsJsonObject();
            for (JsonElement rawPrimitive : mesh.getAsJsonArray("primitives")) {
                JsonObject candidate = rawPrimitive.getAsJsonObject();
                if (candidate.getAsJsonObject("attributes").has("JOINTS_0")) {
                    skinIndex = node.get("skin").getAsInt();
                    primitive = candidate;
                    break;
                }
            }
            if (primitive != null) break;
        }
        if (primitive == null) throw new IllegalArgumentException("No skinned humanoid primitive");
        if (primitive.has("mode") && primitive.get("mode").getAsInt() != 4) {
            throw new IllegalArgumentException("Expected triangle-list primitive");
        }

        JsonObject skin = gltf.getAsJsonArray("skins").get(skinIndex).getAsJsonObject();
        int[] jointNodes = ints(skin.getAsJsonArray("joints"));
        float[][] ibmRows = reader.read(skin.get("inverseBindMatrices").getAsInt());
        Matrix4f[] inverseBind = new Matrix4f[jointNodes.length];
        if (ibmRows.length != jointNodes.length) throw new IllegalArgumentException("Skin bind matrix count mismatch");
        for (int j = 0; j < inverseBind.length; j++) inverseBind[j] = new Matrix4f().set(ibmRows[j]);

        JsonObject attrs = primitive.getAsJsonObject("attributes");
        float[][] positions = reader.read(attrs.get("POSITION").getAsInt());
        float[][] normals = reader.read(attrs.get("NORMAL").getAsInt());
        float[][] uvs = attrs.has("TEXCOORD_0") ? reader.read(attrs.get("TEXCOORD_0").getAsInt()) : null;
        float[][] rawJoints = reader.read(attrs.get("JOINTS_0").getAsInt());
        float[][] weights = reader.read(attrs.get("WEIGHTS_0").getAsInt());
        int[][] joints = new int[positions.length][4];
        for (int v = 0; v < positions.length; v++) {
            for (int k = 0; k < 4; k++) {
                joints[v][k] = (int) rawJoints[v][k];
                if (joints[v][k] < 0 || joints[v][k] >= inverseBind.length)
                    throw new IllegalArgumentException("Vertex references missing joint");
            }
        }
        int[] indices = primitive.has("indices") ? reader.readInts(primitive.get("indices").getAsInt())
                : sequentialIndices(positions.length);
        if (indices.length % 3 != 0) throw new IllegalArgumentException("Triangle index count not divisible by 3");

        Curve[][] curves = new Curve[nodes.length][3];
        JsonArray clips = gltf.getAsJsonArray("animations");
        if (clips == null || clips.isEmpty()) throw new IllegalArgumentException("No skeletal animation");
        JsonObject clip = clips.get(0).getAsJsonObject();
        JsonArray channels = clip.getAsJsonArray("channels");
        JsonArray samplers = clip.getAsJsonArray("samplers");
        float start = Float.POSITIVE_INFINITY;
        float end = Float.NEGATIVE_INFINITY;
        for (JsonElement rawChannel : channels) {
            JsonObject channel = rawChannel.getAsJsonObject();
            JsonObject target = channel.getAsJsonObject("target");
            int node = target.get("node").getAsInt();
            int path = switch (target.get("path").getAsString()) {
                case "translation" -> 0;
                case "rotation" -> 1;
                case "scale" -> 2;
                default -> -1;
            };
            if (path < 0) continue;
            JsonObject sampler = samplers.get(channel.get("sampler").getAsInt()).getAsJsonObject();
            String interpolation = sampler.has("interpolation") ? sampler.get("interpolation").getAsString() : "LINEAR";
            if (!interpolation.equals("STEP") && !interpolation.equals("LINEAR"))
                throw new IllegalArgumentException("Unsupported GLB interpolation: " + interpolation);
            float[] times = scalars(reader.read(sampler.get("input").getAsInt()));
            float[][] values = reader.read(sampler.get("output").getAsInt());
            if (times.length != values.length) throw new IllegalArgumentException("Animation key mismatch");
            if (times.length == 0) continue;
            start = Math.min(start, times[0]);
            end = Math.max(end, times[times.length - 1]);
            curves[node][path] = new Curve(times, values, interpolation.equals("STEP"));
        }
        if (!(end > start)) throw new IllegalArgumentException("Animation duration must be positive");
        int weaponControlNode = -1;
        List<SwordPart> swordParts = new ArrayList<>();
        Vector3f handleMin = null;
        Vector3f handleMax = null;
        Matrix4f[] staticWorlds = new Matrix4f[nodes.length];
        JsonArray meshes = gltf.getAsJsonArray("meshes");
        for (int i = 0; i < sourceNodes.size(); i++) {
            JsonObject node = sourceNodes.get(i).getAsJsonObject();
            String name = node.has("name") ? node.get("name").getAsString() : "";
            if (name.equals("TwoHandedWeapon_ctrl")) weaponControlNode = i;
            int tint = swordTint(name);
            if (tint == 0 || !node.has("mesh")) continue;
            Matrix4f transform = staticWorldMatrix(nodes, i, staticWorlds);
            JsonObject mesh = meshes.get(node.get("mesh").getAsInt()).getAsJsonObject();
            for (JsonElement rawPrimitive : mesh.getAsJsonArray("primitives")) {
                JsonObject bladePrimitive = rawPrimitive.getAsJsonObject();
                if (bladePrimitive.has("mode") && bladePrimitive.get("mode").getAsInt() != 4)
                    throw new IllegalArgumentException("Expected triangle-list sword primitive");
                JsonObject attributes = bladePrimitive.getAsJsonObject("attributes");
                float[][] rawPositions = reader.read(attributes.get("POSITION").getAsInt());
                float[][] rawNormals = reader.read(attributes.get("NORMAL").getAsInt());
                float[][] rawUvs = attributes.has("TEXCOORD_0") ?
                        reader.read(attributes.get("TEXCOORD_0").getAsInt()) : null;
                float[][] bakedPositions = new float[rawPositions.length][3];
                float[][] bakedNormals = new float[rawPositions.length][3];
                for (int v = 0; v < rawPositions.length; v++) {
                    Vector3f position = transform.transformPosition(new Vector3f(
                            rawPositions[v][0], rawPositions[v][1], rawPositions[v][2]));
                    Vector3f normal = transform.transformDirection(new Vector3f(
                            rawNormals[v][0], rawNormals[v][1], rawNormals[v][2])).normalize();
                    bakedPositions[v] = new float[] {position.x, position.y, position.z};
                    bakedNormals[v] = new float[] {normal.x, normal.y, normal.z};
                }
                if ("Handle_lambert1_0".equals(name)) {
                    // Compute a stable pivot from the actual exported, world-space
                    // handle bounds. Scale about the grip, not the sword root.
                    for (float[] p : bakedPositions) {
                        Vector3f point = new Vector3f(p[0], p[1], p[2]);
                        if (handleMin == null) {
                            handleMin = new Vector3f(point);
                            handleMax = new Vector3f(point);
                        } else {
                            handleMin.min(point);
                            handleMax.max(point);
                        }
                    }
                }
                int[] bladeIndices = bladePrimitive.has("indices") ?
                        reader.readInts(bladePrimitive.get("indices").getAsInt()) :
                        sequentialIndices(rawPositions.length);
                if (bladeIndices.length % 3 != 0)
                    throw new IllegalArgumentException("Sword triangle indices are invalid");
                swordParts.add(new SwordPart(bakedPositions, bakedNormals, rawUvs,
                        bladeIndices, tint));
            }
        }
        if (!swordParts.isEmpty() && weaponControlNode < 0)
            throw new IllegalArgumentException("Sword exists but TwoHandedWeapon_ctrl is missing");
        if (!swordParts.isEmpty() && handleMin == null)
            throw new IllegalArgumentException("Sword exists but its handle mesh is missing");
        Vector3f handlePivot = handleMin == null ? null :
                new Vector3f(handleMin).add(handleMax).mul(0.5f);
        return new HumanoidGlbModel(nodes, jointNodes, inverseBind, positions, normals, uvs,
                joints, weights, indices, swordParts.toArray(new SwordPart[0]),
                handlePivot, weaponControlNode, curves, start, end);
    }

    private static int swordTint(String name) {
        return switch (name) {
            case "Blade_lambert1_0" -> 0xFFCCD5DE;     // Steel
            case "Guard_lambert1_0" -> 0xFF8F949B;     // Darker steel
            case "handhold_lambert1_0", "Handle_lambert1_0" -> 0xFF594532;
            case "Pummel_lambert1_0" -> 0xFFA6ACB2;
            default -> 0;
        };
    }

    /** World-space transform of the exported, static weapon hierarchy. */
    private static Matrix4f staticWorldMatrix(Node[] nodes, int index, Matrix4f[] cache) {
        if (cache[index] != null) return cache[index];
        Node n = nodes[index];
        Matrix4f local = new Matrix4f().translation(n.translation[0], n.translation[1], n.translation[2])
                .rotate(new Quaternionf(n.rotation[0], n.rotation[1], n.rotation[2], n.rotation[3]))
                .scale(n.scale[0], n.scale[1], n.scale[2]);
        if (n.parent >= 0) local = new Matrix4f(staticWorldMatrix(nodes, n.parent, cache)).mul(local);
        cache[index] = local;
        return local;
    }

    /** Render the 45-frame Blender clip with fractional-frame interpolation. */
    void render(PoseStack pose, VertexConsumer consumer, int packedLight, int overlay, float elapsedSeconds,
                int color) {
        float time = startTime + elapsedSeconds % (endTime - startTime);
        Matrix4f[] worlds = new Matrix4f[nodes.length];
        Matrix4f[] skinMatrices = new Matrix4f[jointNodes.length];
        for (int j = 0; j < jointNodes.length; j++) {
            skinMatrices[j] = new Matrix4f(worldMatrix(jointNodes[j], time, worlds)).mul(inverseBind[j]);
        }

        float[][] deformed = new float[positions.length][3];
        float[][] deformedNormals = new float[positions.length][3];
        for (int v = 0; v < positions.length; v++) {
            Vector3f bindPosition = new Vector3f(positions[v][0], positions[v][1], positions[v][2]);
            Vector3f bindNormal = new Vector3f(normals[v][0], normals[v][1], normals[v][2]);
            Vector3f result = new Vector3f();
            Vector3f normal = new Vector3f();
            for (int k = 0; k < 4; k++) {
                float w = vertexWeights[v][k];
                if (w < 1e-7f) continue;
                Matrix4f matrix = skinMatrices[vertexJoints[v][k]];
                result.fma(w, matrix.transformPosition(new Vector3f(bindPosition)));
                normal.fma(w, matrix.transformDirection(new Vector3f(bindNormal)));
            }
            normal.normalize();
            deformed[v][0] = result.x;
            deformed[v][1] = result.y;
            deformed[v][2] = result.z;
            deformedNormals[v][0] = normal.x;
            deformedNormals[v][1] = normal.y;
            deformedNormals[v][2] = normal.z;
        }
        // Minecraft's entity Cutout render path consumes quads. Duplicate the
        // triangle's last vertex, matching the existing Giant Rat mesh renderer.
        for (int triangle = 0; triangle < triangleIndices.length; triangle += 3) {
            for (int corner = 0; corner < 4; corner++) {
                int index = triangleIndices[triangle + Math.min(corner, 2)];
                float[] p = deformed[index];
                float[] n = deformedNormals[index];
                float[] uv = uvs == null ? ZERO3 : uvs[index];
                consumer.addVertex(pose.last().pose(), p[0], p[1], p[2])
                        .setColor(color)
                        .setUv(uv[0], uv[1])
                        .setOverlay(overlay)
                        .setLight(packedLight)
                        .setNormal(pose.last(), n[0], n[1], n[2]);
            }
        }
        renderSword(pose, consumer, packedLight, overlay, time, worlds);
    }

    private void renderSword(PoseStack pose, VertexConsumer consumer, int packedLight,
                             int overlay, float time, Matrix4f[] worlds) {
        if (swordParts.length == 0) return;
        // A Child Of constraint in Blender is not a glTF runtime feature. The
        // exported weapon is static at the initial combat pose, but the control
        // itself has baked animation. Reconstruct Child Of's evaluated motion:
        // sword(t) = weaponWorld(t) * inverse(weaponWorld(start)) * sword(start).
        Matrix4f movement = new Matrix4f(worldMatrix(weaponControlNode, time, worlds))
                .mul(inverseWeaponAtStart);
        for (SwordPart part : swordParts) {
            for (int triangle = 0; triangle < part.indices.length; triangle += 3) {
                for (int corner = 0; corner < 4; corner++) {
                    int index = part.indices[triangle + Math.min(corner, 2)];
                    float[] p = part.positions[index];
                    float[] n = part.normals[index];
                    float[] uv = part.uvs == null ? ZERO3 : part.uvs[index];
                    Vector3f vertex = new Vector3f(p[0], p[1], p[2])
                            .sub(swordPivot).mul(SWORD_SCALE).add(swordPivot).add(SWORD_OFFSET);
                    movement.transformPosition(vertex);
                    Vector3f normal = movement.transformDirection(new Vector3f(n[0], n[1], n[2])).normalize();
                    consumer.addVertex(pose.last().pose(), vertex.x, vertex.y, vertex.z)
                            .setColor(part.tint)
                            .setUv(uv[0], uv[1])
                            .setOverlay(overlay)
                            .setLight(packedLight)
                            .setNormal(pose.last(), normal.x, normal.y, normal.z);
                }
            }
        }
    }

    private Matrix4f worldMatrix(int index, float time, Matrix4f[] worlds) {
        if (worlds[index] != null) return worlds[index];
        Node n = nodes[index];
        float[] t = sample(curves[index][0], time, n.translation, false);
        float[] r = sample(curves[index][1], time, n.rotation, true);
        float[] s = sample(curves[index][2], time, n.scale, false);
        Matrix4f local = new Matrix4f().translation(t[0], t[1], t[2])
                .rotate(new Quaternionf(r[0], r[1], r[2], r[3]))
                .scale(s[0], s[1], s[2]);
        if (n.parent >= 0) local = new Matrix4f(worldMatrix(n.parent, time, worlds)).mul(local);
        worlds[index] = local;
        return local;
    }

    private static float[] sample(Curve curve, float time, float[] rest, boolean quaternion) {
        if (curve == null) return rest;
        float[] times = curve.times;
        if (time <= times[0]) return curve.values[0];
        int last = times.length - 1;
        if (time >= times[last]) return curve.values[last];
        int upper = Arrays.binarySearch(times, time);
        if (upper >= 0) return curve.values[upper];
        upper = -upper - 1;
        int lower = upper - 1;
        if (curve.step) return curve.values[lower];
        float alpha = (time - times[lower]) / (times[upper] - times[lower]);
        float[] a = curve.values[lower];
        float[] b = curve.values[upper];
        if (quaternion) {
            Quaternionf q = new Quaternionf(a[0], a[1], a[2], a[3]);
            q.slerp(new Quaternionf(b[0], b[1], b[2], b[3]), alpha);
            return new float[] {q.x, q.y, q.z, q.w};
        }
        float[] out = new float[a.length];
        for (int i = 0; i < out.length; i++) out[i] = a[i] + alpha * (b[i] - a[i]);
        return out;
    }

    private static float readCalibration(String name, float fallback, float min, float max) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) return fallback;
        try {
            float number = Float.parseFloat(value);
            if (Float.isFinite(number) && number >= min && number <= max) return number;
        } catch (NumberFormatException ignored) { }
        LOGGER.warn("Ignoring invalid {}={} (expected {} to {}); using {}", name, value, min, max, fallback);
        return fallback;
    }

    private static float[] scalars(float[][] value) {
        float[] result = new float[value.length];
        for (int i = 0; i < result.length; i++) result[i] = value[i][0];
        return result;
    }

    private static float[] floats(JsonObject object, String field, float[] fallback) {
        if (!object.has(field)) return fallback.clone();
        JsonArray values = object.getAsJsonArray(field);
        float[] result = new float[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i).getAsFloat();
        return result;
    }

    private static int[] ints(JsonArray array) {
        int[] result = new int[array.size()];
        for (int i = 0; i < result.length; i++) result[i] = array.get(i).getAsInt();
        return result;
    }

    private static int[] sequentialIndices(int count) {
        int[] result = new int[count];
        for (int i = 0; i < count; i++) result[i] = i;
        return result;
    }

    /** Binary accessor reader: supports interleaved streams and GLB component types used by the export. */
    private static final class Accessors {
        private final JsonArray views;
        private final JsonArray accessors;
        private final ByteBuffer buffer;

        Accessors(JsonObject gltf, ByteBuffer buffer) {
            this.views = gltf.getAsJsonArray("bufferViews");
            this.accessors = gltf.getAsJsonArray("accessors");
            this.buffer = buffer;
        }

        float[][] read(int id) {
            JsonObject a = accessors.get(id).getAsJsonObject();
            JsonObject view = views.get(a.get("bufferView").getAsInt()).getAsJsonObject();
            int type = a.get("componentType").getAsInt();
            int count = a.get("count").getAsInt();
            int components = components(a.get("type").getAsString());
            int size = componentSize(type);
            int stride = view.has("byteStride") ? view.get("byteStride").getAsInt() : components * size;
            int base = offset(view) + offset(a);
            boolean normalized = a.has("normalized") && a.get("normalized").getAsBoolean();
            float[][] output = new float[count][components];
            for (int i = 0; i < count; i++) for (int c = 0; c < components; c++) {
                int pos = base + i * stride + c * size;
                output[i][c] = switch (type) {
                    case 5121 -> normalized ? (buffer.get(pos) & 255) / 255f : (buffer.get(pos) & 255);
                    case 5123 -> normalized ? (buffer.getShort(pos) & 65535) / 65535f : (buffer.getShort(pos) & 65535);
                    case 5125 -> (float) Integer.toUnsignedLong(buffer.getInt(pos));
                    case 5126 -> buffer.getFloat(pos);
                    default -> throw new IllegalArgumentException("Unsupported accessor component type " + type);
                };
            }
            return output;
        }

        int[] readInts(int id) {
            float[][] values = read(id);
            int[] output = new int[values.length];
            for (int i = 0; i < output.length; i++) output[i] = (int) values[i][0];
            return output;
        }

        private static int offset(JsonObject object) {
            return object.has("byteOffset") ? object.get("byteOffset").getAsInt() : 0;
        }

        private static int componentSize(int type) {
            return switch (type) {
                case 5120, 5121 -> 1;
                case 5122, 5123 -> 2;
                case 5125, 5126 -> 4;
                default -> throw new IllegalArgumentException("Unsupported accessor component type " + type);
            };
        }

        private static int components(String type) {
            return switch (type) {
                case "SCALAR" -> 1;
                case "VEC2" -> 2;
                case "VEC3" -> 3;
                case "VEC4" -> 4;
                case "MAT4" -> 16;
                default -> throw new IllegalArgumentException("Unsupported accessor shape " + type);
            };
        }
    }
}
