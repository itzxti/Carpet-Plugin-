package com.ishland.bukkit.carpetplugin.commands;

import com.ishland.bukkit.carpetplugin.lib.fakeplayer.base.FakeEntityPlayer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.literal;
import static com.ishland.bukkit.carpetplugin.utils.BrigadierUtils.sendMessage;

public class ListCommand {

    public void register() {
        CommandDispatcher<CommandSourceStack> dispatcher =
                ((CraftServer) Bukkit.getServer()).getHandle().getServer()
                        .getCommands().getDispatcher();
        dispatcher.register(literal("list")
                .then(literal("carpet")
                        .then(literal("removeOrphan")
                                .executes(ListCommand::removeOrphan)
                        )
                        .executes(ListCommand::listCarpet)
                )
        );
    }

    private static int removeOrphan(CommandContext<CommandSourceStack> ctx) {
        PlayerList playerList = ((CraftServer) Bukkit.getServer()).getServer().getPlayerList();
        List<ServerPlayer> players = new CopyOnWriteArrayList<>(playerList.getPlayers());
        for (ServerPlayer player : players) {
            if (playerList.getPlayerByName(player.getName().getString()) != player) {
                ctx.getSource().getBukkitSender().sendMessage(
                        "Trying to remove orphan: " + player.getName().getString()
                        + " (" + player.getClass().getName() + ")"
                );
                try {
                    player.connection.disconnect(Component.literal("Remove orphan"));
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }
        return 1;
    }

    private static int listCarpet(CommandContext<CommandSourceStack> ctx) {
        PlayerList playerList = ((CraftServer) Bukkit.getServer()).getServer().getPlayerList();
        List<ServerPlayer> players = new CopyOnWriteArrayList<>(playerList.getPlayers());
        sendMessage(ctx, "There are " + players.size() + " of a max of " + playerList.getMaxPlayers() + " players online:");
        for (ServerPlayer player : players) {
            boolean isOrphan = playerList.getPlayerByName(player.getName().getString()) != player;
            String type = player instanceof FakeEntityPlayer ? "CarpetPlugin player" : "Others (" + player.getClass().getName() + ")";
            String prefix = isOrphan ? "[Possible Orphan] " : "";
            sendMessage(ctx, prefix + type + " " + player.getName().getString() + " (UUID: " + player.getUUID() + ")");
        }
        return 1;
    }
}
