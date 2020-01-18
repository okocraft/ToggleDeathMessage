package net.okocraft.deathmessages.listener;


import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import net.okocraft.deathmessages.DeathMessages;
import net.okocraft.deathmessages.NMSDeathMessageGetter;

public class PlayerDeathListener implements Listener {

    private static DeathMessages plugin = DeathMessages.getInstance();
    private static PlayerDeathListener instance;

    private PlayerDeathListener() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static void start() {
        if (!isRunning()) {
            instance = new PlayerDeathListener();
        }
    }

    public static boolean isRunning() {
        return instance != null;
    }

    public static void stop() {
        if (isRunning()) {
            HandlerList.unregisterAll(instance);
            instance = null;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onTest(PlayerDeathEvent event) {
        event.getEntity().spigot().sendMessage(NMSDeathMessageGetter.getDeathMessage(event));
        event.getEntity().sendMessage("send nms death message");
    }

}