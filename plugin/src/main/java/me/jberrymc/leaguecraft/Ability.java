package me.jberrymc.leaguecraft;

/** The four champion abilities and the hotbar slot (0-based) the datapack keeps each one in. */
enum Ability {
    Q(0), W(1), E(2), R(3);

    private final int slot;

    Ability(int slot) {
        this.slot = slot;
    }

    int slot() {
        return slot;
    }

    /** Lower-case id used in datapack function names (use_q, use_w ...). */
    String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    static Ability fromSlot(int slot) {
        for (Ability ability : values()) {
            if (ability.slot == slot) return ability;
        }
        return null;
    }
}
