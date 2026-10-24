#!/usr/bin/env python3
"""Extract an Animated Java rig's bones (rest pose) into a JSON file the plugin can load.

Reads the rig's summon function from the generated Animated Java datapack, where every bone is an
item_display passenger of the root with its item and rest-pose transformation, and writes:

    {"rig": "aatrox", "bones": [{"name": "horn1", "item": "minecraft:allay_spawn_egg",
      "model": 2.0, "display": "head", "transformation": [16 floats, row-major]}, ...]}

Usage (from the repo root):
    python3 plugin/tools/extract_aj_rig.py aatrox
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
AJ = ROOT / "datapack/generated/leaguecraft-animated-java/data/animated_java/function"
OUT = ROOT / "plugin/src/main/resources/rigs"

BONE = re.compile(
    r'\{id:"minecraft:item_display",Tags:\[[^\]]*"aj\.(?P<rig>[\w.]+)\.bone\.(?P<name>[\w.]+)"\],'
    r'transformation:\[(?P<matrix>[^\]]*)\](?P<rest>.*?)(?=\},\{id:"minecraft:item_display"|\}\]\}$)')


def main(rig: str) -> None:
    summon = (AJ / rig / "summon.mcfunction").read_text().splitlines()[0]
    bones = []
    for match in BONE.finditer(summon):
        rest = match["rest"]
        item = re.search(r'item:\{id:"([^"]+)"', rest)[1]
        model = float(re.search(r'"minecraft:custom_model_data":\{floats:\[([-\d.]+)f\]', rest)[1])
        display = re.search(r'item_display:"(\w+)"', rest)[1]
        matrix = [float(v.rstrip("f")) for v in match["matrix"].split(",")]
        if len(matrix) != 16:
            raise SystemExit(f"bone {match['name']}: expected 16 matrix values, got {len(matrix)}")
        bones.append({"name": match["name"], "item": item, "model": model,
                      "display": display, "transformation": matrix})
    if not bones:
        raise SystemExit(f"no bones found in {rig}/summon.mcfunction")
    OUT.mkdir(parents=True, exist_ok=True)
    target = OUT / f"{rig}.json"
    target.write_text(json.dumps({"rig": rig, "bones": bones}, indent=1) + "\n")
    print(f"{target.relative_to(ROOT)}: {len(bones)} bones")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "aatrox")
