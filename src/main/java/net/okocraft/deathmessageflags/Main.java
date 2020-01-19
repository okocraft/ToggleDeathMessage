package net.okocraft.deathmessageflags;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	private static Main instance;

	private StateFlag sendDeathMessageFlag;
    private StateFlag listenDeathMessageFlag;
	
	@Override
	public void onLoad() {
		sendDeathMessageFlag = registerStateFlag("send-death-message", true);
		listenDeathMessageFlag = registerStateFlag("listen-death-message", true);
	}

	private StateFlag registerStateFlag(String name, boolean def) {
		try {
			// If this plugin is already loaded, current state is not onLoad().
			getInstance();
			return null;
		} catch (IllegalStateException ignore) {
		}

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
				return null;
			}
		}
	}

	@Override
	public void onEnable() {
		PlayerDeathListener.start();
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

	StateFlag getSendDeathMessageFlag() {
		return sendDeathMessageFlag;
	}

	StateFlag getListenDeathMessageFlag() {
		return listenDeathMessageFlag;
	}
}
