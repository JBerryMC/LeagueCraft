const fs = require("fs");
const path = require("path");

// Target pack format. 121 = Minecraft 26.3 (LeagueCraft was originally built for 1.20.2, format 18).
// Change this when updating, and update min_format/max_format in pack.mcmeta to match.
// (MC-Build only reads the old pack_format field, so the version has to be set here.)
const FORMAT = 121;

// ---------------------------------------------------------------------------
// static/ holds the few files MC-Build 4.2 can't write correctly for this pack:
//   - damage types and damage-type tags (mcb writes them to the wrong folder)
//   - item modifiers that are JSON arrays (mcb's JSON syntax only does objects)
// They are copied into data/ before every build, with folder names renamed for
// the target version (Minecraft 1.21 / format 48 dropped the plural folder names).
// ---------------------------------------------------------------------------
const RENAMES_48 = {
	functions: "function",
	advancements: "advancement",
	item_modifiers: "item_modifier",
	loot_tables: "loot_table",
	predicates: "predicate",
	recipes: "recipe",
	structures: "structure",
};
const TAG_RENAMES_48 = {
	functions: "function",
	blocks: "block",
	entity_types: "entity_type",
	fluids: "fluid",
	game_events: "game_event",
	items: "item",
};

function mapPath(rel) {
	// rel = ["data", namespace, type, ...]
	if (FORMAT < 48 || rel[0] !== "data" || rel.length < 4) return rel;
	const out = rel.slice();
	if (out[2] === "tags") {
		out[3] = TAG_RENAMES_48[out[3]] ?? out[3];
	} else {
		out[2] = RENAMES_48[out[2]] ?? out[2];
	}
	return out;
}

function walk(dir, base = dir, found = []) {
	if (!fs.existsSync(dir)) return found;
	for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
		const full = path.join(dir, entry.name);
		if (entry.isDirectory()) walk(full, base, found);
		else found.push(path.relative(base, full).split(path.sep));
	}
	return found;
}

function copyStatic() {
	const staticDir = path.join(__dirname, "static");
	const manifestFile = path.join(__dirname, ".static-files.json");
	const previous = fs.existsSync(manifestFile) ? JSON.parse(fs.readFileSync(manifestFile, "utf8")) : [];
	const written = [];
	for (const rel of walk(staticDir)) {
		const target = path.join(__dirname, ...mapPath(rel));
		fs.mkdirSync(path.dirname(target), { recursive: true });
		fs.copyFileSync(path.join(staticDir, ...rel), target);
		written.push(path.relative(__dirname, target));
	}
	// remove files copied by an earlier build that no longer exist in static/
	for (const old of previous) {
		if (!written.includes(old)) fs.rmSync(path.join(__dirname, old), { force: true });
	}
	fs.writeFileSync(manifestFile, JSON.stringify(written, null, 1));
}

copyStatic();

module.exports = {
	libDir: null,
	generatedDirName: "zzz",
	internalScoreboardName: "mcb.internal",
	header: null,
	ioThreadCount: 1,
	setup: null,
	formatVersion: FORMAT,
};
