package me.jberrymc.leaguecraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.List;

/** /lc (status), /lc keys, /lc dev, /lc rigtest, /lc reload, /lc debug on|off */
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

        commands.register(root.build(), "LeagueCraft plugin status and controls", List.of("lc"));
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
        send(ctx, "<gray>Basic attack: <white>hitscan</white> (melee " + c.meleeRange() + ", ranged " + c.rangedRange() + " blocks)");
        return Command.SINGLE_SUCCESS;
    }

    private static String line(String label, boolean value) {
        return "<gray>" + label + ": " + (value ? "<green>yes" : "<red>no");
    }

    private static void send(CommandContext<CommandSourceStack> ctx, String miniMessage) {
        ctx.getSource().getSender().sendMessage(MM.deserialize(miniMessage));
    }
}
