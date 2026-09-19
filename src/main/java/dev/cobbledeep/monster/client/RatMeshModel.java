package dev.cobbledeep.monster.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads the custom polygon-and-keyframe export from the editable Blockbench project. */
final class RatMeshModel {
    private static final float DEGREES = (float) Math.PI / 180f;
    private static final float[] ZERO = {0, 0, 0};
    private static final float[] ONE = {1, 1, 1};

    private final List<Node> roots;
    private final Map<String, Clip> clips;
    private final float textureWidth;
    private final float textureHeight;

    private RatMeshModel(List<Node> roots, Map<String, Clip> clips, float width, float height) {
        this.roots = roots;
        this.clips = clips;
        this.textureWidth = width;
        this.textureHeight = height;
    }

    static RatMeshModel load(ResourceManager resources, ResourceLocation location) {
        try (var stream = resources.open(location);
             var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
            if (!"cobbledeep.mesh_animation.v1".equals(data.get("schema").getAsString())) {
                throw new IllegalArgumentException("Unsupported mesh format: " + location);
            }
            JsonObject groups = data.getAsJsonObject("groups");
            JsonObject meshes = data.getAsJsonObject("meshes");
            List<Node> roots = new ArrayList<>();
            for (JsonElement raw : data.getAsJsonArray("hierarchy")) {
                roots.add(readNode(raw.getAsJsonObject(), groups, meshes));
            }
            Map<String, Clip> clips = new HashMap<>();
            for (var entry : data.getAsJsonObject("animations").entrySet()) {
                clips.put(entry.getKey(), Clip.read(entry.getValue().getAsJsonObject()));
            }
            JsonObject texture = data.getAsJsonObject("texture");
            return new RatMeshModel(roots, clips,
                    texture.get("width").getAsFloat(), texture.get("height").getAsFloat());
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load rat geometry and animations: " + location, error);
        }
    }

    void render(PoseStack pose, VertexConsumer output, int light, int overlay,
                String animation, float elapsedSeconds) {
        render(pose, output, light, overlay, animation, elapsedSeconds, 0xFFFFFFFF);
    }

    /** Render the same posed meshes with a per-vertex ARGB tint for optional highlights. */
    void render(PoseStack pose, VertexConsumer output, int light, int overlay,
                String animation, float elapsedSeconds, int color) {
        Clip clip = clips.get(animation);
        float time = clip == null ? 0 : clip.loop && clip.length > 0
                ? elapsedSeconds % clip.length : Math.min(elapsedSeconds, clip.length);
        for (Node root : roots) {
            renderNode(root, clip, time, pose, output, light, overlay, color);
        }
    }

    private void renderNode(Node node, Clip clip, float time, PoseStack pose,
                            VertexConsumer output, int light, int overlay, int color) {
        pose.pushPose();
        if (node.bone != null) {
            Bone bone = node.bone;
            Channel channel = clip == null ? null : clip.channels.get(bone.id);
            float[] translation = channel == null ? ZERO : sample(channel.position, time, ZERO);
            float[] rotation = channel == null ? ZERO : sample(channel.rotation, time, ZERO);
            float[] scale = channel == null ? ONE : sample(channel.scale, time, ONE);
            // Bone pivots use model-global Blockbench coordinates. Moving back by the
            // pivot after rotation keeps parent/child joints attached during animation.
            pose.translate(bone.origin[0] + translation[0], bone.origin[1] + translation[1],
                    bone.origin[2] + translation[2]);
            rotate(pose, bone.rotation[0] + rotation[0], bone.rotation[1] + rotation[1],
                    bone.rotation[2] + rotation[2]);
            pose.scale(scale[0], scale[1], scale[2]);
            pose.translate(-bone.origin[0], -bone.origin[1], -bone.origin[2]);
        }
        if (node.mesh != null) {
            Mesh mesh = node.mesh;
            pose.pushPose();
            // Unlike bone pivots, a Blockbench free-model mesh origin is its actual
            // element translation. Cancelling it would separate the legs and torso.
            pose.translate(mesh.origin[0], mesh.origin[1], mesh.origin[2]);
            rotate(pose, mesh.rotation[0], mesh.rotation[1], mesh.rotation[2]);
            for (Face face : mesh.faces) {
                // The entity render type expects quads. Duplicate a triangle's last corner.
                for (int corner = 0; corner < 4; corner++) {
                    int index = Math.min(corner, face.positions.length - 1);
                    float[] point = face.positions[index];
                    float[] uv = face.uvs[index];
                    output.addVertex(pose.last().pose(), point[0], point[1], point[2])
                            .setColor(color)
                            .setUv(uv[0] / textureWidth, uv[1] / textureHeight)
                            .setOverlay(overlay)
                            .setLight(light)
                            .setNormal(pose.last(), face.normal[0], face.normal[1], face.normal[2]);
                }
            }
            pose.popPose();
        }
        for (Node child : node.children) {
            renderNode(child, clip, time, pose, output, light, overlay, color);
        }
        pose.popPose();
    }

    private static void rotate(PoseStack pose, float x, float y, float z) {
        if (x != 0 || y != 0 || z != 0) {
            pose.mulPose(new Quaternionf().rotationXYZ(x * DEGREES, y * DEGREES, z * DEGREES));
        }
    }

    private static Node readNode(JsonObject raw, JsonObject groups, JsonObject meshes) {
        if (raw.has("mesh")) {
            return new Node(null, Mesh.read(meshes.getAsJsonObject(raw.get("mesh").getAsString())),
                    List.of());
        }
        String id = raw.get("bone").getAsString();
        JsonObject data = groups.getAsJsonObject(id);
        Bone bone = new Bone(id, vector(data.getAsJsonArray("origin")),
                vector(data.getAsJsonArray("rotation")));
        List<Node> children = new ArrayList<>();
        for (JsonElement child : raw.getAsJsonArray("children")) {
            children.add(readNode(child.getAsJsonObject(), groups, meshes));
        }
        return new Node(bone, null, children);
    }

    private record Node(Bone bone, Mesh mesh, List<Node> children) {}
    private record Bone(String id, float[] origin, float[] rotation) {}
    private record Face(float[][] positions, float[][] uvs, float[] normal) {}

    private record Mesh(float[] origin, float[] rotation, List<Face> faces) {
        static Mesh read(JsonObject data) {
            JsonObject vertices = data.getAsJsonObject("vertices");
            List<Face> faces = new ArrayList<>();
            for (var entry : data.getAsJsonObject("faces").entrySet()) {
                JsonObject face = entry.getValue().getAsJsonObject();
                JsonArray ids = face.getAsJsonArray("vertices");
                if (ids.size() != 3 && ids.size() != 4) continue;
                float[][] positions = new float[ids.size()][];
                float[][] uvs = new float[ids.size()][];
                JsonObject uvMap = face.getAsJsonObject("uv");
                for (int i = 0; i < ids.size(); i++) {
                    String id = ids.get(i).getAsString();
                    positions[i] = vector(vertices.getAsJsonArray(id));
                    uvs[i] = vector(uvMap.getAsJsonArray(id));
                }
                Vector3f normal = new Vector3f(positions[1][0] - positions[0][0],
                        positions[1][1] - positions[0][1], positions[1][2] - positions[0][2]);
                normal.cross(new Vector3f(positions[2][0] - positions[0][0],
                        positions[2][1] - positions[0][1], positions[2][2] - positions[0][2]));
                if (normal.lengthSquared() < 1e-10f) normal.set(0, 1, 0);
                else normal.normalize();
                faces.add(new Face(positions, uvs, new float[]{normal.x, normal.y, normal.z}));
            }
            return new Mesh(vector(data.getAsJsonArray("origin")),
                    vector(data.getAsJsonArray("rotation")), faces);
        }
    }

    private record Frame(float time, float[] value) {}
    private record Channel(List<Frame> rotation, List<Frame> position, List<Frame> scale) {
        static Channel read(JsonObject data) {
            return new Channel(frames(data, "rotation"), frames(data, "position"), frames(data, "scale"));
        }
    }
    private record Clip(float length, boolean loop, Map<String, Channel> channels) {
        static Clip read(JsonObject data) {
            Map<String, Channel> channels = new HashMap<>();
            for (var entry : data.getAsJsonObject("channels").entrySet()) {
                channels.put(entry.getKey(), Channel.read(entry.getValue().getAsJsonObject()));
            }
            return new Clip(data.get("length").getAsFloat(),
                    data.get("loop").getAsBoolean(), channels);
        }
    }

    private static float[] vector(JsonArray values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = values.get(i).getAsFloat();
        return result;
    }

    private static List<Frame> frames(JsonObject data, String name) {
        if (!data.has(name)) return List.of();
        List<Frame> frames = new ArrayList<>();
        for (JsonElement value : data.getAsJsonArray(name)) {
            JsonObject frame = value.getAsJsonObject();
            frames.add(new Frame(frame.get("time").getAsFloat(),
                    vector(frame.getAsJsonArray("value"))));
        }
        frames.sort(Comparator.comparingDouble(Frame::time));
        return frames;
    }

    private static float[] sample(List<Frame> frames, float time, float[] rest) {
        if (frames.isEmpty()) return rest;
        if (time <= frames.get(0).time) return frames.get(0).value;
        for (int i = 1; i < frames.size(); i++) {
            Frame next = frames.get(i);
            if (time <= next.time) {
                Frame previous = frames.get(i - 1);
                float delta = next.time - previous.time;
                float alpha = delta <= 0 ? 1 : Math.max(0, Math.min(1, (time - previous.time) / delta));
                return new float[]{previous.value[0] + (next.value[0] - previous.value[0]) * alpha,
                        previous.value[1] + (next.value[1] - previous.value[1]) * alpha,
                        previous.value[2] + (next.value[2] - previous.value[2]) * alpha};
            }
        }
        return frames.get(frames.size() - 1).value;
    }
}
