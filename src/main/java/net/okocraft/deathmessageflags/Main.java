package net.okocraft.deathmessageflags;

import java.util.List;
import java.util.Objects;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.deathmessageflags.config.PlayerData;
import net.okocraft.deathmessageflags.listener.PlayerDeathListener;

public class Main extends JavaPlugin {

	private static Main instance;

	private StateFlag sendDeathMessageFlag;
    private StateFlag receiveDeathMessageFlag;
	
	@Override
	public void onLoad() {
		sendDeathMessageFlag = registerStateFlag("send-death-message", true);
		receiveDeathMessageFlag = registerStateFlag("receive-death-message", true);
	}

	private StateFlag registerStateFlag(String name, boolean def) {
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		try {
			StateFlag flag = new StateFlag(name, def);
			registry.register(flag);
			return flag;
		} catch (FlagConflictException e) {
			// some other plugin registered a flag by the same name already.
			// you can use the existing flag, but this may cause conflicts - be sure to check type
			Flag<?> existing = registry.get(name);
			if (existing instanceof StateFlag) {
				return (StateFlag) existing;
			} else {
				// types don't match - this is bad news! some other plugin conflicts with you
				// hopefully this never actually happens
				Bukkit.getPluginManager().disablePlugin(this);
				return null;
			}
		}
	}

	@Override
	public void onEnable() {
		PlayerDeathListener.start();
		PluginCommand command = Objects.requireNonNull(getCommand("toggledeathmessage"));
		command.setExecutor(this);
		command.setTabCompleter(this);
	}

	@Override
	public void onDisable() {
		PlayerDeathListener.stop();
	};

	/**
	 * Gets plugin instance.
	 * 
	 * @return Instance of DeathMessages plugin.
	 * @throws IllegalStateException If this plugin is not enabled.
	 */
	public static Main getInstance() throws IllegalStateException {
		if (instance == null) {
			instance = (Main) Bukkit.getPluginManager().getPlugin("DeathMessageFlags");
			if (instance == null) {
				throw new IllegalStateException("Plugin is not enabled!");
			}
		}
		return instance;
	}

	public StateFlag getSendDeathMessageFlag() {
		return sendDeathMessageFlag;
	}

	public StateFlag getReceiveDeathMessageFlag() {
		return receiveDeathMessageFlag;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			PlayerData.getInstance().setHidingDeathMessage(
				(Player) sender,
				!PlayerData.getInstance().isHidingDeathMessage((Player) sender)
			);
		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return List.of();
	}
}
