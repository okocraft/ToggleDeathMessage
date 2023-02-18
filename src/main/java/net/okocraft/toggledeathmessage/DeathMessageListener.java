package net.okocraft.toggledeathmessage;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.kyori.adventure.text.Component;
import org.bukkit.GameRule;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.scoreboard.Team;

public class DeathMessageListener implements Listener {

    public static final String METADATA_KEY = "no_see_death_message";

    private final Main plugin;

    public DeathMessageListener(Main plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(ignoreCancelled = true)
    private void putMetaOnPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Boolean showDeathMessage = player.getWorld().getGameRuleValue(GameRule.SHOW_DEATH_MESSAGES);
        if (showDeathMessage == null || !showDeathMessage || event.getDeathMessage() == null) {
            return;
        }

        MetadataValue noSeeMeta = new FixedMetadataValue(
                Optional.ofNullable(plugin.getServer().getPluginManager().getPlugin("MoreFlags")).orElse(plugin),
                null
        );

        plugin.getServer().getOnlinePlayers().forEach(p -> {
            if (plugin.getPlayerData().isHidingDeathMessage(p)) {
                p.setMetadata(METADATA_KEY, noSeeMeta);
            }
        });

    }

    /**
     * Filters player death message without protocollib.<br>
     * This method will have some internal side effects since it removes original death message and resend it.<br>
     * For example:
     * <ul>
     * <li>Death reason displayed on death screen will disappear</li>
     * <li>Break plugins hiding or modifying death messages with same way.</li>
     * </ul>
     *
     * @param event player death event.
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void filterMessageOnPlayerDeath(PlayerDeathEvent event) {
        if (plugin.getServer().getPluginManager().getPlugin("MoreFlags") != null) {
            return;
        }

        Set<Player> onlinePlayers = new HashSet<>(plugin.getServer().getOnlinePlayers());
        if (onlinePlayers.isEmpty()) {
            return;
        }

        Player player = event.getEntity();
        Team team = player.getScoreboard().getPlayerTeam(player);
        if (team != null) {
            Team.OptionStatus deathMessageOption = team.getOption(Team.Option.DEATH_MESSAGE_VISIBILITY);
            switch (deathMessageOption) {
                case ALWAYS:
                    break;
                case FOR_OTHER_TEAMS:
                    // Hide for other teams or victim
                    onlinePlayers.removeIf(onlinePlayer -> onlinePlayer.equals(player)
                            || team.equals(onlinePlayer.getScoreboard().getEntryTeam(onlinePlayer.getName())));
                    break;
                case FOR_OWN_TEAM:
                    // Hide for own team
                    onlinePlayers.removeIf(onlinePlayer -> !team.equals(onlinePlayer.getScoreboard().getEntryTeam(onlinePlayer.getName())));
                    break;
                default:
                    return;
            }
        }

        try {
            // Paper
            Component deathMessage = event.deathMessage();
            if (deathMessage == null) {
                return;
            }
            event.deathMessage(null);
            for (Player onlinePlayer : onlinePlayers) {
                onlinePlayer.sendMessage(deathMessage);
            }
        } catch (NoClassDefFoundError | NoSuchMethodError e) {
            // Spigot
            String deathMessage = event.getDeathMessage();
            if (deathMessage == null) {
                return;
            }
            event.setDeathMessage(null);
            for (Player onlinePlayer : onlinePlayers) {
                onlinePlayer.sendMessage(deathMessage);
            }
        }
    }

    /**
     * Triggers removing meta AFTER player death event listener with MONITOR priority.
     *
     * @param event event when fired player's statistic is changed.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerStatisticIncrement(PlayerStatisticIncrementEvent event) {
        if (plugin.getServer().getPluginManager().getPlugin("MoreFlags") != null) {
            return;
        }
        if (event.getStatistic() == Statistic.DEATHS) {
            plugin.getServer().getOnlinePlayers().forEach(p -> p.removeMetadata(METADATA_KEY, plugin));
        }
    }
}