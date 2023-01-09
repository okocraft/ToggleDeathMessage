package net.okocraft.toggledeathmessage.config;

import net.okocraft.toggledeathmessage.Main;
import org.bukkit.OfflinePlayer;

public final class PlayerData extends CustomConfig {

    public PlayerData(Main plugin) {
        super(plugin, "playerdata.yml");
    }

    public boolean isHidingDeathMessage(OfflinePlayer player) {
        return get().getBoolean("hide-death-message." + player.getUniqueId(), false);
    }

    public void setHidingDeathMessage(OfflinePlayer player, boolean isHidden) {
        get().set("hide-death-message." + player.getUniqueId(), isHidden ? true : null);
        save();
    }
}