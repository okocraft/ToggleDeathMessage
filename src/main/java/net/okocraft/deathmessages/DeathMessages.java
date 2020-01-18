package net.okocraft.deathmessages;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import net.okocraft.deathmessages.listener.PlayerDeathListener;


public class DeathMessages extends JavaPlugin {

	private static DeathMessages instance;

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
	public static DeathMessages getInstance() throws IllegalStateException {
		if (instance == null) {
			instance = (DeathMessages) Bukkit.getPluginManager().getPlugin("DeathMessages");
			if (instance == null) {
				throw new IllegalStateException("Plugin is not enabled!");
			}
		}
		return instance;
	}
}
