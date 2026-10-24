package net.okocraft.toggledeathmessage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.OfflinePlayer;
import org.jspecify.annotations.Nullable;

final class PlayerData {

    static PlayerData load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return new PlayerData(Set.of());
        }

        try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
            return new PlayerData(lines.map(PlayerData::parseToUuid).filter(Objects::nonNull).toList());
        }
    }

    private static @Nullable UUID parseToUuid(String str) {
        try {
            return UUID.fromString(str.strip());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean dirty = new AtomicBoolean();

    private PlayerData(Collection<UUID> uuids) {
        this.hiddenPlayers.addAll(uuids);
    }

    boolean isHidingDeathMessage(OfflinePlayer player) {
        return hiddenPlayers.contains(player.getUniqueId());
    }

    void setHidingDeathMessage(OfflinePlayer player, boolean hidden) {
        boolean changed = hidden ? hiddenPlayers.add(player.getUniqueId()) : hiddenPlayers.remove(player.getUniqueId());
        if (changed) {
            dirty.set(true);
        }
    }

    void save(Path file) throws IOException {
        if (!dirty.compareAndSet(true, false)) {
            return;
        }

        try {
            Files.createDirectories(file.getParent());
            Files.write(file, hiddenPlayers.stream().map(UUID::toString).toList(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            dirty.set(true);
            throw e;
        }
    }
}
