package net.okocraft.deathmessageflags.config;

import org.bukkit.OfflinePlayer;

public final class PlayerData extends CustomConfig {

    private static final PlayerData INSTANCE = new PlayerData();

    PlayerData() {
        super("playerdata.yml");
    }

    public static PlayerData getInstance() {
        return INSTANCE;
    }

    public boolean isHidingDeathMessage(OfflinePlayer player) {
        return get().getBoolean("hide-death-message." + player.getUniqueId(), false);
    }

    public void setHidingDeathMessage(OfflinePlayer player, boolean isHidden) {
        get().set("hide-death-message." + player.getUniqueId(), isHidden ? true : null);
        save();
    }
}