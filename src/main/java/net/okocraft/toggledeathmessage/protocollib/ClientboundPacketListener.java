package net.okocraft.toggledeathmessage.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.okocraft.toggledeathmessage.Main;
import org.bukkit.entity.Player;

public class ClientboundPacketListener extends PacketAdapter {

    private static final PacketType[] PACKET_TYPES_TO_LISTEN = {
            PacketType.Play.Server.SYSTEM_CHAT
    };

    private final Main plugin;

    ClientboundPacketListener(Main plugin) {
        super(plugin, PACKET_TYPES_TO_LISTEN);
        this.plugin = plugin;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SYSTEM_CHAT) {
            onSystemChatPacket(
                    event.getPlayer(),
                    event.getPacket().getStrings().read(0),
                    event.getPacket().getBooleans().read(0),
                    event
            );
        }
    }

    private void onSystemChatPacket(Player client, String content, boolean overlay, PacketEvent event) {
        if (content != null
                && GsonComponentSerializer.gson().deserialize(content) instanceof TranslatableComponent translatable
                && translatable.key().startsWith("death.")
                && plugin.getPlayerData().isHidingDeathMessage(client)) {
            event.setCancelled(true);
        }
    }
}
