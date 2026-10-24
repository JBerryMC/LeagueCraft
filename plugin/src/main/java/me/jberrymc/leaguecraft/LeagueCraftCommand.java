package me.jberrymc.leaguecraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /lc (status), /lc keys, /lc dev, /lc rigtest, /lc reload, /lc debug on|off, and the match
 * controls: /lc queue join|leave|start|skip|end, /lc pick (champion), /lc team red|blue,
 * /lc surrender, /lc dummy red|blue|clear.
 */
final class LeagueCraftCommand {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private LeagueCraftCommand() {}

    static void register(Commands commands, LeagueCraftPlugin plugin) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("leaguecraft")
                .executes(ctx -> status(ctx, plugin));

        root.then(Commands.literal("keys").executes(ctx -> {
            send(ctx, LeagueCraftPlugin.KEY_SETUP_TIP);
            return Command.SINGLE_SUCCESS;
        }));

        // No .requires() on the executor: requirements are checked against whoever typed the
        // command, which would block "execute as <player> run lc dev" from the console.
        root.then(Commands.literal("dev")
                .requires(source -> source.getSender().hasPermission("leaguecraft.dev"))
                .executes(ctx -> {
                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                        send(ctx, "<red>Run this as a player (or: execute as <player> run lc dev).");
                        return 0;
                    }
                    boolean on = plugin.dev().toggle(player);
                    player.sendMessage(MM.deserialize(on
                            ? "<green>Dev mode on.</green> <gray>Every input you make shows up here with what it did. /lc dev again to turn off."
                            : "<gray>Dev mode off."));
                    return Command.SINGLE_SUCCESS;
                }));

        root.then(Commands.literal("rigtest")
                .requires(source -> source.getSender().hasPermission("leaguecraft.dev"))
                .executes(ctx -> {
                    if (!(ctx.getSource().getExecutor() instanceof Player player)) {
                        send(ctx, "<red>Run this as a player (or: execute as <player> run lc rigtest).");
                        return 0;
                    }
                    RigTest rigs = plugin.rigTest();
                    if (rigs == null) {
                        send(ctx, "<red>No rig loaded; see the server log.");
                        return 0;
                    }
                    boolean on = rigs.toggle(player);
                    player.sendMessage(MM.deserialize(on
                            ? "<green>Mounted the " + rigs.rigName() + " rig on you.</green> <gray>Press F5 to see it. "
                                    + (plugin.dev().on(player) ? "Animation changes show here."
                                    : "Turn on <white>/lc dev</white> to see animation changes.")
                                    + " /lc rigtest again to take it off."
                            : "<gray>Rig taken off."));
                    return Command.SINGLE_SUCCESS;
                }));

        root.then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("leaguecraft.admin"))
                .executes(ctx -> {
                    plugin.reload();
                    send(ctx, "<green>LeagueCraft config reloaded.");
                    return Command.SINGLE_SUCCESS;
                }));

        for (boolean on : new boolean[] {true, false}) {
            root.then(Commands.literal("debug").then(Commands.literal(on ? "on" : "off")
                    .requires(source -> source.getSender().hasPermission("leaguecraft.admin"))
                    .executes(ctx -> {
                        plugin.setDebug(on);
                        send(ctx, "<green>Cast logging " + (on ? "on" : "off") + ".");
                        return Command.SINGLE_SUCCESS;
                    })));
        }

        registerGame(root, plugin);
        commands.register(root.build(), "LeagueCraft plugin status and controls", List.of("lc"));
    }

    static final List<String> CHAMPIONS = List.of("aatrox", "annie", "ashe", "ezreal", "garen", "jhin", "leesin",
            "leona", "lux", "malphite", "soraka", "vayne", "yasuo", "yone", "zed");

    /**
     * Shortcuts to the datapack's own match controls: queue, champion pick, team, surrender and
     * practice dummies. Each runs the same function the datapack's menu or book would.
     */
    private static void registerGame(LiteralArgumentBuilder<CommandSourceStack> root, LeagueCraftPlugin plugin) {
        Datapack datapack = plugin.datapack();
        var queue = Commands.literal("queue");
        queue.then(Commands.literal("join").executes(ctx -> withPlayer(ctx, player -> {
            if (datapack.matchRunning()) return fail(ctx, "A match is already running.");
            if (player.getScoreboardTags().contains("is_in_queue")) return fail(ctx, "You are already in the queue.");
            if (datapack.score("players", "queue", 0) >= datapack.score("max", "queue", 10)) return fail(ctx, "The queue is full.");
            datapack.runAs(player, "lc:game/queue/join");
            return Command.SINGLE_SUCCESS;
        })));
        queue.then(Commands.literal("leave").executes(ctx -> withPlayer(ctx, player -> {
            if (!player.getScoreboardTags().contains("is_in_queue")) return fail(ctx, "You are not in the queue.");
            datapack.runAs(player, "lc:game/queue/leave");
            return Command.SINGLE_SUCCESS;
        })));
        queue.then(Commands.literal("start").requires(admin()).executes(ctx -> {
            if (datapack.matchRunning()) return fail(ctx, "A match is already running.");
            if (datapack.score("players", "queue", 0) <= 0) return fail(ctx, "Nobody is in the queue (/lc queue join).");
            datapack.command("function lc:game/queue/start");
            send(ctx, "<green>Queue started: champion select is open.");
            return Command.SINGLE_SUCCESS;
        }));
        queue.then(Commands.literal("skip").requires(admin()).executes(ctx -> {
            if (!datapack.matchRunning() || datapack.score("waittime", "queue", 0) <= 0) {
                return fail(ctx, "Nothing to skip: champion select is not counting down.");
            }
            datapack.command("scoreboard players set waittime queue 0");
            send(ctx, "<green>Skipped champion select; the match starts now.");
            return Command.SINGLE_SUCCESS;
        }));
        queue.then(Commands.literal("end").requires(admin()).executes(ctx -> {
            if (datapack.matchRunning()) {
                datapack.command("function lc:game/game/end");
                send(ctx, "<green>Match ended.");
            } else {
                datapack.command("execute as @a[tag=is_in_queue] run function lc:game/queue/leave");
                send(ctx, "<green>Queue emptied.");
            }
            return Command.SINGLE_SUCCESS;
        }));
        root.then(queue);

        root.then(Commands.literal("pick").then(Commands.argument("champion", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    CHAMPIONS.stream().filter(c -> c.startsWith(builder.getRemainingLowerCase())).forEach(builder::suggest);
                    return builder.buildFuture();
                })
                .executes(ctx -> withPlayer(ctx, player -> {
                    String champion = StringArgumentType.getString(ctx, "champion").toLowerCase(java.util.Locale.ROOT);
                    if (!CHAMPIONS.contains(champion)) return fail(ctx, "No champion called " + champion + ".");
                    boolean selecting = datapack.score(player, "settings.page", 0) == 100;
                    if (!selecting && !ctx.getSource().getSender().hasPermission("leaguecraft.dev")) {
                        return fail(ctx, "You can pick a champion during champion select.");
                    }
                    datapack.runAs(player, "lc:menu/champion/pick {name:\"" + champion + "\"}");
                    if (!player.getScoreboardTags().contains(champion)) {
                        return fail(ctx, "Could not pick " + champion + ": a teammate already has it.");
                    }
                    return Command.SINGLE_SUCCESS;
                }))));

        var team = Commands.literal("team");
        for (String color : List.of("red", "blue")) {
            team.then(Commands.literal(color).executes(ctx -> withPlayer(ctx, player -> {
                if (player.getScoreboardTags().contains("champion") && datapack.matchRunning()) {
                    return fail(ctx, "You can not change teams during a match.");
                }
                datapack.command("team join " + color + " " + player.getName());
                send(ctx, "<green>You are on the " + color + " team.");
                return Command.SINGLE_SUCCESS;
            })));
        }
        root.then(team);

        root.then(Commands.literal("surrender").executes(ctx -> withPlayer(ctx, player -> {
            if (!datapack.matchRunning() || !player.getScoreboardTags().contains("champion")) {
                return fail(ctx, "You can only surrender during a match.");
            }
            datapack.runAs(player, "lc:menu/move/surrender");
            return Command.SINGLE_SUCCESS;
        })));

        var dummy = Commands.literal("dummy").requires(source -> source.getSender().hasPermission("leaguecraft.dev"));
        for (String color : List.of("red", "blue")) {
            dummy.then(Commands.literal(color).executes(ctx -> withPlayer(ctx, player -> {
                datapack.runAs(player, "lc:core/dummy/spawn." + color);
                send(ctx, "<green>Spawned a " + color + " practice dummy.");
                return Command.SINGLE_SUCCESS;
            })));
        }
        dummy.then(Commands.literal("clear").executes(ctx -> {
            datapack.command("function lc:core/dummy/clear");
            send(ctx, "<green>Practice dummies cleared.");
            return Command.SINGLE_SUCCESS;
        }));
        root.then(dummy);
    }

    private static java.util.function.Predicate<CommandSourceStack> admin() {
        return source -> source.getSender().hasPermission("leaguecraft.admin");
    }

    private static int withPlayer(CommandContext<CommandSourceStack> ctx, java.util.function.ToIntFunction<Player> action) {
        if (!(ctx.getSource().getExecutor() instanceof Player player)) {
            send(ctx, "<red>Run this as a player (or: execute as <player> run lc ...).");
            return 0;
        }
        return action.applyAsInt(player);
    }

    private static int fail(CommandContext<CommandSourceStack> ctx, String message) {
        send(ctx, "<red>" + message);
        return 0;
    }

    private static int status(CommandContext<CommandSourceStack> ctx, LeagueCraftPlugin plugin) {
        InputSettings s = plugin.settings();
        Datapack datapack = plugin.datapack();
        send(ctx, "<gold><bold>LeagueCraft</bold></gold> <gray>v" + plugin.getPluginMeta().getVersion());
        send(ctx, line("Datapack detected", datapack.installed()));
        send(ctx, line("Match running", datapack.matchRunning()));
        send(ctx, line("Only during a match", s.requireMatch()));
        StringBuilder hotbar = new StringBuilder();
        for (int slot = 0; slot < 9; slot++) {
            KeyAction action = s.hotbar().get(slot);
            if (action != null) hotbar.append(" ").append(slot + 1).append("=").append(action.id());
        }
        send(ctx, "<gray>Drop: <white>" + s.drop().id() + "</white>  F: <white>" + s.swapHands().id()
                + "</white>  Hotbar:<white>" + hotbar);
        CombatSettings c = plugin.combat();
        send(ctx, line("Third person (camera distance " + c.cameraDistance() + ", crosshair " + (c.crosshair() ? "on" : "off") + ")",
                c.thirdPerson()));
        send(ctx, line("Zoom on right click (view narrower: melee " + Math.round(c.zoomMelee() * 100) + "%, ranged "
                + Math.round(c.zoomRanged() * 100) + "%"
                + "; slower: melee " + Math.round(c.zoomSlowMelee() * 100) + "%, ranged " + Math.round(c.zoomSlowRanged() * 100) + "%"
                + (c.zoomChampions().isEmpty() ? "" : ", " + c.zoomChampions().size() + " champion overrides") + ")", c.zoom()));
        send(ctx, line("No sprinting", c.disableSprint()) + "  " + line("No crouching", c.disableSneak()));
        send(ctx, "<gray>Basic attack: <white>melee cone " + Math.round(c.meleeAngle()) + "°, ranged hitscan</white>"
                + " (range, attack speed and ammo from the stats chart) · reload key: <white>"
                + c.reloadKey().name().toLowerCase(java.util.Locale.ROOT));
        return Command.SINGLE_SUCCESS;
    }

    private static String line(String label, boolean value) {
        return "<gray>" + label + ": " + (value ? "<green>yes" : "<red>no");
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String miniMessage) {
        ctx.getSource().getSender().sendMessage(MM.deserialize(miniMessage));
    }
}
