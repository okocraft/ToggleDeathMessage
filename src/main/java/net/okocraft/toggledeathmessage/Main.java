package net.okocraft.toggledeathmessage;

import java.util.List;
import java.util.Objects;
import net.okocraft.toggledeathmessage.config.PlayerData;
import net.okocraft.toggledeathmessage.protocollib.ProtocolLibHook;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Main extends JavaPlugin {

	private final ProtocolLibHook protocolLibHook = new ProtocolLibHook(this);

	private PlayerData playerData;

	@Override
	public void onEnable() {
		playerData = new PlayerData(this);

		if (!protocolLibHook.registerHandlers()) {
			throw new RuntimeException("ProtocolLib not found!");
		}

		PluginCommand command = Objects.requireNonNull(getCommand("toggledeathmessage"));
		command.setExecutor(this);
		command.setTabCompleter(this);
	}

	@Override
	public void onDisable() {
		if (protocolLibHook.hasProtocolLib()) {
			protocolLibHook.unregisterHandlers();
		}
	};

	public PlayerData getPlayerData() {
		return playerData;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		if (sender instanceof Player) {
			boolean nextState = !playerData.isHidingDeathMessage((Player) sender);
			playerData.setHidingDeathMessage(
				(Player) sender,
				nextState
			);
			sender.sendMessage(nextState ? "§7death message turned §aoff" : "§7death message turned §con");
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
		return List.of();
	}
}
