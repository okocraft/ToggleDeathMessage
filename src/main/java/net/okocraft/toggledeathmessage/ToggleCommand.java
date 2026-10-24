package net.okocraft.toggledeathmessage;

import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

final class ToggleCommand {

    static final String PERMISSION = "toggledeathmessage.toggle";

    private ToggleCommand() {
        throw new UnsupportedOperationException();
    }

    static void register(Commands commands, PlayerData playerData) {
        var node = Commands.literal("toggledeathmessage")
            .requires(source -> source.getExecutor() instanceof Player && source.getSender().hasPermission(PERMISSION))
            .executes(ctx -> execute(ctx.getSource(), playerData, null))
            .then(Commands.literal("on").executes(ctx -> execute(ctx.getSource(), playerData, true)))
            .then(Commands.literal("off").executes(ctx -> execute(ctx.getSource(), playerData, false)))
            .build();

        commands.register(node, "Toggles whether you can see death messages.", List.of("tdm"));
    }

    /**
     * @param show {@code true} to show, {@code false} to hide, {@code null} to toggle
     */
    private static int execute(CommandSourceStack source, PlayerData playerData, @Nullable Boolean show) {
        if (!(source.getExecutor() instanceof Player player)) {
            return 0;
        }

        boolean visible = show != null ? show : playerData.isHidingDeathMessage(player);
        playerData.setHidingDeathMessage(player, !visible);

        player.sendMessage(
            Component.text("Death messages turned ", NamedTextColor.GRAY)
                .append(visible ? Component.text("on", NamedTextColor.RED) : Component.text("off", NamedTextColor.GREEN))
        );
        return Command.SINGLE_SUCCESS;
    }
}
