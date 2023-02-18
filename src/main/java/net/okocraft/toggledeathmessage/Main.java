package net.okocraft.toggledeathmessage;

import java.util.List;
import java.util.Objects;
import net.okocraft.toggledeathmessage.config.PlayerData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Main extends JavaPlugin {

	private PlayerData playerData;

	@Override
	public void onEnable() {
		playerData = new PlayerData(this);

		getServer().getPluginManager().registerEvents(new DeathMessageListener(this), this);

		PluginCommand command = Objects.requireNonNull(getCommand("toggledeathmessage"));
		command.setExecutor(this);
		command.setTabCompleter(this);
	}

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
