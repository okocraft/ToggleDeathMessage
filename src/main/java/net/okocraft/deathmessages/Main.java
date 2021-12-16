package net.okocraft.deathmessages;

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

import net.okocraft.deathmessages.config.PlayerData;

public class Main extends JavaPlugin {

	private PlayerData playerData;
	private PlayerDeathListener playerDeathListener;

	private StateFlag sendDeathMessageFlag;
    private StateFlag receiveDeathMessageFlag;
    private StateFlag imprisonDeathMessageFlag;
	
	@Override
	public void onLoad() {
		sendDeathMessageFlag = registerStateFlag("send-death-message", true);
		receiveDeathMessageFlag = registerStateFlag("receive-death-message", true);
		receiveDeathMessageFlag = registerStateFlag("imprison-death-message", false);
	}

	private StateFlag registerStateFlag(String name, boolean def) {
		FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		try {
			StateFlag flag = new StateFlag(name, def);
			registry.register(flag);
			return flag;
		} catch (FlagConflictException | IllegalStateException e) {
			// some other plugin registered a flag by the same name already.
			// you can use the existing flag, but this may cause conflicts - be sure to check type
			Flag<?> existing = registry.get(name);
			if (existing instanceof StateFlag) {
				return (StateFlag) existing;
			} else {
				// types don't match - this is bad news! some other plugin conflicts with you
				// hopefully this never actually happens
				Bukkit.getPluginManager().disablePlugin(this);
				System.out.println("flag is null.");
				return null;
			}
		}
	}

	@Override
	public void onEnable() {
		playerData = new PlayerData(this);
		playerDeathListener = new PlayerDeathListener(this);
		playerDeathListener.start();

		PluginCommand command = Objects.requireNonNull(getCommand("toggledeathmessage"));
		command.setExecutor(this);
		command.setTabCompleter(this);
	}

	@Override
	public void onDisable() {
		playerDeathListener.stop();
	};

	public PlayerData getPlayerData() {
		return playerData;
	}

	public StateFlag getSendDeathMessageFlag() {
		return sendDeathMessageFlag;
	}

	public StateFlag getReceiveDeathMessageFlag() {
		return receiveDeathMessageFlag;
	}

	public StateFlag getImprisonDeathMessageFlag() {
		return imprisonDeathMessageFlag;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return List.of();
	}
}
