package me.jberrymc.leaguecraft;

import java.util.Locale;

/** What an input does. Mapped from Minecraft actions in config.yml (keys: ...). */
enum KeyAction {
    Q(Ability.Q), W(Ability.W), E(Ability.E), R(Ability.R),
    SUMMONER_1(null), SUMMONER_2(null),
    NONE(null);

    private final Ability ability;

    KeyAction(Ability ability) {
        this.ability = ability;
    }

    /** The ability this casts, or null for summoner spells and NONE. */
    Ability ability() {
        return ability;
    }

    /** Config spelling: q, w, e, r, summoner-1, summoner-2, none. */
    String id() {
        return name().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    /** The action that casts this ability. */
    static KeyAction of(Ability ability) {
        return switch (ability) {
            case Q -> Q;
            case W -> W;
            case E -> E;
            case R -> R;
        };
    }

    static KeyAction parse(String id) {
        if (id == null) return null;
        String normal = id.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (KeyAction action : values()) {
            if (action.id().equals(normal)) return action;
        }
        return null;
    }
}
