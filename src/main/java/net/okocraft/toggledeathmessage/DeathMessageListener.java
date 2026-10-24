package net.okocraft.toggledeathmessage;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Team;

final class DeathMessageListener implements Listener {

    /**
     * Metadata key set by other plugins (e.g. MoreFlags) on players who must not receive the current death message.
     * It is consumed (removed from every online player) at the end of each death event handled here.
     */
    private static final String HIDE_METADATA_KEY = "no_see_death_message";

    private final Plugin plugin;
    private final PlayerData playerData;

    DeathMessageListener(Plugin plugin, PlayerData playerData) {
        this.plugin = plugin;
        this.playerData = playerData;
    }

    /**
     * Cancels the vanilla broadcast and re-sends the death message only to players who have not hidden it.
     * <p>
     * The death screen keeps the original message through {@link PlayerDeathEvent#deathScreenMessageOverride(Component)}.
     * This may conflict with other plugins that hide or modify death messages in the same way.
     *
     * @param event the player death event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        try {
            filterDeathMessage(event);
        } finally {
            clearHideMetadata();
        }
    }

    private void filterDeathMessage(PlayerDeathEvent event) {
        Component deathMessage = event.deathMessage();
        if (deathMessage == null || !event.getShowDeathMessages()) {
            return;
        }

        var onlinePlayers = plugin.getServer().getOnlinePlayers();
        if (onlinePlayers.stream().noneMatch(this::isHiding)) {
            return;
        }

        Player victim = event.getPlayer();
        List<Player> recipients = new ArrayList<>(onlinePlayers.size());
        for (Player player : onlinePlayers) {
            if (!isHiding(player) && canSeeByTeamRule(victim, player)) {
                recipients.add(player);
            }
        }

        if (event.deathScreenMessageOverride() == null) {
            event.deathScreenMessageOverride(deathMessage);
        }
        event.deathMessage(null);

        plugin.getServer().getConsoleSender().sendMessage(deathMessage);
        recipients.forEach(player -> player.sendMessage(deathMessage));
    }

    private boolean isHiding(Player player) {
        return playerData.isHidingDeathMessage(player) || player.hasMetadata(HIDE_METADATA_KEY);
    }

    @SuppressWarnings("deprecation") // The metadata API is deprecated, but it is the contract with MoreFlags.
    private void clearHideMetadata() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            for (MetadataValue value : player.getMetadata(HIDE_METADATA_KEY)) {
                player.removeMetadata(HIDE_METADATA_KEY, value.getOwningPlugin());
            }
        }
    }

    /**
     * Reproduces vanilla's handling of the {@code deathMessageVisibility} team option.
     */
    private boolean canSeeByTeamRule(Player victim, Player viewer) {
        Team team = victim.getScoreboard().getEntryTeam(victim.getName());
        if (team == null) {
            return true;
        }

        return switch (team.getOption(Team.Option.DEATH_MESSAGE_VISIBILITY)) {
            case ALWAYS -> true;
            case NEVER -> false;
            // Hidden for other teams: only the victim's teammates (excluding the victim) can see it.
            case FOR_OTHER_TEAMS -> !viewer.equals(victim) && team.hasEntry(viewer.getName());
            // Hidden for own team: only players outside the victim's team can see it.
            case FOR_OWN_TEAM -> !team.hasEntry(viewer.getName());
        };
    }
}
