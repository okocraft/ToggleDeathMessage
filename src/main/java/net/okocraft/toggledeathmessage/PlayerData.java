package net.okocraft.toggledeathmessage;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PlayerData {

    public static PlayerData load(Main plugin) throws IOException {
        if (Files.isRegularFile(plugin.getDataFilepath())) {
            try (var lines = Files.lines(plugin.getDataFilepath(), StandardCharsets.UTF_8)) {
                return new PlayerData(
                        lines.map(PlayerData::parseToUuid)
                                .filter(Objects::nonNull)
                                .toList()
                );
            }
        } else {
            var old = plugin.getDataFolder().toPath().resolve("playerdata.yml");

            if (Files.isRegularFile(old)) {
                var data = loadFromOldPlayerDataFile(old.toFile());
                data.save(plugin.getDataFilepath());
                return data;
            } else {
                return new PlayerData();
            }
        }
    }

    private static PlayerData loadFromOldPlayerDataFile(File file) {
        var section = YamlConfiguration.loadConfiguration(file).getConfigurationSection("hide-death-message");

        if (section == null) {
            return new PlayerData();
        }

        return new PlayerData(toUUIDs(section.getKeys(false)));
    }

    private static List<UUID> toUUIDs(Collection<String> col) {
        var result = new ArrayList<UUID>(col.size());

        for (var e : col) {
            var parsed = parseToUuid(e);

            if (parsed != null) {
                result.add(parsed);
            }
        }

        return result;
    }

    private static @Nullable UUID parseToUuid(String str) {
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean dirty = new AtomicBoolean();

    private PlayerData() {
    }

    private PlayerData(Collection<UUID> uuids) {
        this.hiddenPlayers.addAll(uuids);
    }

    public boolean isHidingDeathMessage(OfflinePlayer player) {
        return hiddenPlayers.contains(player.getUniqueId());
    }

    public void setHidingDeathMessage(OfflinePlayer player, boolean isHidden) {
        if (isHidden) {
            dirty.compareAndSet(false, hiddenPlayers.add(player.getUniqueId()));
        } else {
            dirty.compareAndSet(false, hiddenPlayers.remove(player.getUniqueId()));
        }
    }

    public void save(Path file) throws IOException {
        Set<UUID> players;

        if (dirty.compareAndSet(true, false)) {
            players = Set.copyOf(hiddenPlayers);
        } else {
            return;
        }

        try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            for (var uuid : players) {
                writer.write(uuid.toString());
                writer.newLine();
            }
        }
    }
}
