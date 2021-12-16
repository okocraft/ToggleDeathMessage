package net.okocraft.deathmessages;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StateFlag.State;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
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

import net.kyori.adventure.text.Component;

public class PlayerDeathListener implements Listener {

    private final Main plugin;
    private RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();

    PlayerDeathListener(Main plugin) {
        this.plugin = plugin;
    }
    
    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void stop() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Component deathMessage = event.deathMessage();

        if (!player.getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES) || deathMessage == null) {
            return;
        }
        
        event.deathMessage(null);

        if (!calcStateFlag(player, plugin.getSendDeathMessageFlag())) {
            return;
        }

        Set<Player> onlinePlayers;
        
        if (calcStateFlag(player, plugin.getImprisonDeathMessageFlag())) {
            Set<ProtectedRegion> playerRegions = new HashSet<>(getRegions(player).getRegions());
            playerRegions.removeIf(region -> region.getFlag(plugin.getImprisonDeathMessageFlag()) != State.ALLOW);
            onlinePlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(onlinePlayer -> playerRegions.stream().anyMatch(region -> region.contains(
                            BukkitAdapter.adapt(onlinePlayer.getLocation()).toVector().toBlockPoint()
                    )))
                    .filter(onlinePlayer -> calcStateFlag(onlinePlayer, plugin.getReceiveDeathMessageFlag()))
                    .filter(onlinePlayer -> !plugin.getPlayerData().isHidingDeathMessage(onlinePlayer))
                    .collect(Collectors.toSet());
        } else {
            onlinePlayers = Bukkit.getOnlinePlayers().stream()
                    .filter(onlinePlayer -> calcStateFlag(onlinePlayer, plugin.getReceiveDeathMessageFlag()))
                    .filter(onlinePlayer -> !plugin.getPlayerData().isHidingDeathMessage(onlinePlayer))
                    .collect(Collectors.toSet());
        }


        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team != null) {
            OptionStatus deathMessageOption = team.getOption(Option.DEATH_MESSAGE_VISIBILITY);
            switch (deathMessageOption) {
                case ALWAYS:
                    break;
                case FOR_OTHER_TEAMS:
                    // Hide for other teams or victim
                    onlinePlayers.removeIf(onlinePlayer -> onlinePlayer.equals(player)
                            || onlinePlayer.getScoreboard().getEntryTeam(onlinePlayer.getName()).equals(team));
                    break;
                case FOR_OWN_TEAM:
                    // Hide for own team
                    onlinePlayers.removeIf(onlinePlayer -> !onlinePlayer.getScoreboard().getEntryTeam(onlinePlayer.getName()).equals(team));
                    break;
                default:
                    return;
            }
        }

        for (Player onlinePlayer : onlinePlayers) {
            onlinePlayer.sendMessage(deathMessage);
        }
    }

    private boolean calcStateFlag(Player player, StateFlag flag) {
        return getRegions(player).testState(WorldGuardPlugin.inst().wrapPlayer(player), flag);
    }

    private ApplicableRegionSet getRegions(Player player) {
        return regionContainer.createQuery().getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
    }

}