package net.okocraft.toggledeathmessage;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;
import org.jspecify.annotations.Nullable;

public final class ToggleDeathMessagePlugin extends JavaPlugin {

    private final Path dataFilepath = getDataPath().resolve("players.dat");
    private @Nullable PlayerData playerData;
    private @Nullable ScheduledTask saveTask;

    @Override
    public void onEnable() {
        try {
            playerData = PlayerData.load(dataFilepath);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Failed to load player data from " + dataFilepath, e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        var data = playerData;
        getServer().getPluginManager().registerEvents(new DeathMessageListener(this, data), this);
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> ToggleCommand.register(event.registrar(), data));

        saveTask = getServer().getAsyncScheduler().runAtFixedRate(this, $ -> savePlayerData(), 1L, 1L, TimeUnit.MINUTES);
    }

    @Override
    public void onDisable() {
        if (saveTask != null) {
            saveTask.cancel();
            saveTask = null;
        }

        if (playerData != null) {
            savePlayerData();
        }
    }

    private void savePlayerData() {
        if (playerData == null) {
            return;
        }

        try {
            playerData.save(dataFilepath);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save player data to " + dataFilepath, e);
        }
    }
}
