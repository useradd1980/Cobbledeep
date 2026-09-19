#!/usr/bin/env python3
"""Export Cobbledeep's editable Generic Model (.bbmodel) to runtime JSON + PNG.

Usage: python3 tools/export_giant_rat.py /path/to/cobbledeep_giant_rat_animated.bbmodel
Run from the repository root. Standard library only; does not change your source project.
"""
import argparse
import base64
import json
from pathlib import Path


def export(source: Path, root: Path):
    project = json.loads(source.read_text(encoding="utf-8"))
    if project.get("meta", {}).get("model_format") != "free":
        raise ValueError("Expected a Blockbench Generic Model (polygon-mesh) project")
    texture = project["textures"][0]
    marker = "data:image/png;base64,"
    if not texture["source"].startswith(marker):
        raise ValueError("Texture must be embedded as a PNG in the Blockbench project")
    png = base64.b64decode(texture["source"][len(marker):], validate=True)
    if not png.startswith(b"\x89PNG\r\n\x1a\n"):
        raise ValueError("Embedded texture is not a PNG")

    meshes = {mesh["uuid"]: {field: mesh[field] for field in (
        "name", "origin", "rotation", "vertices", "faces")}
        for mesh in project["elements"] if mesh.get("type") == "mesh"}
    groups = {group["uuid"]: {field: group[field] for field in (
        "name", "origin", "rotation")} for group in project["groups"]}
    assigned = []

    def tree(nodes):
        result = []
        for node in nodes:
            if isinstance(node, str):
                if node not in meshes:
                    raise ValueError("Unknown mesh referenced in Outliner: " + node)
                assigned.append(node)
                result.append({"mesh": node})
            else:
                uid = node["uuid"]
                if uid not in groups:
                    raise ValueError("Unknown rig group: " + uid)
                result.append({"bone": uid, "children": tree(node.get("children", []))})
        return result

    hierarchy = tree(project["outliner"])
    if len(assigned) != len(meshes) or len(assigned) != len(set(assigned)):
        raise ValueError("Every mesh must occur once in the rig hierarchy")
    animations = {}
    for animation in project["animations"]:
        channels = {}
        for bone_id, animator in animation["animators"].items():
            if bone_id not in groups:
                raise ValueError("Unknown animation bone: " + bone_id)
            tracks = {}
            for channel in ("rotation", "position", "scale"):
                frames = [{
                    "time": key["time"],
                    "value": [float(key["data_points"][0][axis]) for axis in ("x", "y", "z")],
                    "interpolation": key.get("interpolation", "linear")
                } for key in animator.get("keyframes", []) if key["channel"] == channel]
                if frames:
                    tracks[channel] = sorted(frames, key=lambda frame: frame["time"])
            if tracks:
                channels[bone_id] = tracks
        animations[animation["name"]] = {
            "length": animation["length"],
            "loop": animation["loop"] == "loop",
            "channels": channels
        }
    payload = {
        "schema": "cobbledeep.mesh_animation.v1",
        "texture": {"path": "giant_rat.png", "width": texture["width"], "height": texture["height"]},
        "groups": groups, "hierarchy": hierarchy, "meshes": meshes, "animations": animations
    }
    assets = root / "src/main/resources/assets/cobbledeep"
    model_file = assets / "models/entity/giant_rat.json"
    texture_file = assets / "textures/entity/giant_rat.png"
    model_file.parent.mkdir(parents=True, exist_ok=True)
    texture_file.parent.mkdir(parents=True, exist_ok=True)
    model_file.write_text(json.dumps(payload, separators=(",", ":")) + "\n", encoding="utf-8")
    texture_file.write_bytes(png)
    print(f"Exported {len(meshes)} meshes, {len(groups)} rig groups and {len(animations)} clips")
    print(model_file)
    print(texture_file)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("bbmodel", type=Path, help="The finished, editable Giant Rat .bbmodel")
    args = parser.parse_args()
    export(args.bbmodel, Path(__file__).resolve().parent.parent)
