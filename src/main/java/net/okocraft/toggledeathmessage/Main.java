package net.okocraft.toggledeathmessage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class Main extends JavaPlugin {

	private final Path dataFilepath = getDataFolder().toPath().resolve("players.dat");
	private PlayerData playerData;
	private Runnable cancellingSaveTask;

	@Override
	public void onEnable() {
		try {
			playerData = PlayerData.load(this);
		} catch (IOException e) {
			getLogger().log(Level.SEVERE, "Failed to initialize PlayerData.", e);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		getServer().getPluginManager().registerEvents(new DeathMessageListener(this), this);

		PluginCommand command = Objects.requireNonNull(getCommand("toggledeathmessage"));
		command.setExecutor(this);
		command.setTabCompleter(this);

		scheduleSaveTask();
	}

	@Override
	public void onDisable() {
		if (cancellingSaveTask != null) {
			cancellingSaveTask.run();
			savePlayerData();
		}
	}

	public Path getDataFilepath() {
		return dataFilepath;
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

	private void scheduleSaveTask() {
		try {
			// Paper 1.20.1 or Folia
			getServer().getClass().getDeclaredMethod("getAsyncScheduler");
			var task = getServer().getAsyncScheduler().runAtFixedRate(this, $ -> savePlayerData(), 1L, 1L, TimeUnit.MINUTES);
			cancellingSaveTask = task::cancel;
		} catch (NoSuchMethodException ignored) {
			// Old Paper or Spigot
			long ticks = 60L * 50L;
			var task = getServer().getScheduler().runTaskTimerAsynchronously(this, this::scheduleSaveTask, ticks, ticks);
			cancellingSaveTask = task::cancel;
		}
	}

    private void savePlayerData() {
        try {
            playerData.save(getDataFilepath());
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save player data to " + getDataFilepath().getFileName(), e);
        }
    }
}
