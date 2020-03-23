package net.okocraft.deathmessageflags.listener;

import java.util.Set;
import java.util.stream.Collectors;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scoreboard.Team;
import org.bukkit.scoreboard.Team.Option;
import org.bukkit.scoreboard.Team.OptionStatus;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.okocraft.deathmessageflags.Main;
import net.okocraft.deathmessageflags.NMSDeathMessageGetter;
import net.okocraft.deathmessageflags.config.PlayerData;

public class PlayerDeathListener implements Listener {

    private static Main plugin = Main.getInstance();
    private static RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (PlayerData.getInstance().isHidingDeathMessage(player)) {
            event.setDeathMessage(null);
            return;
        }

        if (!player.getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES) || event.getDeathMessage() == null || event.getDeathMessage().equals("")) {
            return;
        }
        
        BaseComponent deathMessageComponent = NMSDeathMessageGetter.getDeathMessage(event);
        if (!event.getDeathMessage().equals(deathMessageComponent.toPlainText())) {
            deathMessageComponent = new TextComponent(event.getDeathMessage());
        }

        event.setDeathMessage(null);

        if (!calcStateFlag(player, plugin.getSendDeathMessageFlag())) {
            return;
        }
        Set<Player> onlinePlayers = Bukkit.getOnlinePlayers().stream()
                .filter(onlinePlayer -> calcStateFlag(onlinePlayer, plugin.getReceiveDeathMessageFlag()))
                .collect(Collectors.toSet());

        Team team = player.getScoreboard().getEntryTeam(player.getName());
        OptionStatus deathMessageOption = team.getOption(Option.DEATH_MESSAGE_VISIBILITY);
        if (team != null && deathMessageOption != OptionStatus.ALWAYS) {
            if (deathMessageOption == OptionStatus.FOR_OTHER_TEAMS) {
                // Hide for other teams or victim
                for (Player onlinePlayer : onlinePlayers) {
                    if (!onlinePlayer.equals(player) && onlinePlayer.getScoreboard().getEntryTeam(onlinePlayer.getName()).equals(team)) {
                        onlinePlayer.spigot().sendMessage(deathMessageComponent);
                    }
                }
            } else if (deathMessageOption == OptionStatus.FOR_OWN_TEAM) {
                // Hide for own team
                for (Player onlinePlayer : onlinePlayers) {
                    if (!onlinePlayer.getScoreboard().getEntryTeam(onlinePlayer.getName()).equals(team)) {
                        onlinePlayer.spigot().sendMessage(deathMessageComponent);
                    }
                }
            }
        } else {
            for (Player onlinePlayer : onlinePlayers) {
                onlinePlayer.spigot().sendMessage(deathMessageComponent);
            }
        }
    }

    private boolean calcStateFlag(Player player, StateFlag flag) {
        RegionManager rm = regionContainer.get(BukkitAdapter.adapt(player.getWorld()));
        ApplicableRegionSet applicableRegionSet = rm.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()).toVector().toBlockPoint());
        return applicableRegionSet.testState(WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }
}