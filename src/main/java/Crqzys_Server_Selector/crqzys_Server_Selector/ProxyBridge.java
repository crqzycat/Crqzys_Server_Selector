package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.logging.Level;

/**
 * Talks to the proxy over the legacy {@code BungeeCord} plugin channel, which both BungeeCord/Waterfall
 * and Velocity (with {@code bungee-plugin-message-channel} enabled) understand.
 * <p>
 * Messages are written with plain {@code DataOutputStream} rather than Guava's {@code ByteStreams} so
 * the plugin does not depend on a library that the server API is free to stop shipping.
 */
final class ProxyBridge implements PluginMessageListener {

    private static final String CHANNEL = "BungeeCord";

    private final Crqzys_Server_Selector plugin;

    /** Name the proxy knows this server by, or {@code null} until the proxy answers. */
    private volatile String currentServer;

    private boolean registered;

    ProxyBridge(Crqzys_Server_Selector plugin) {
        this.plugin = plugin;
    }

    void register() {
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(plugin, CHANNEL);
        messenger.registerIncomingPluginChannel(plugin, CHANNEL, this);
        registered = true;
    }

    void unregister() {
        if (!registered) {
            return;
        }
        Messenger messenger = plugin.getServer().getMessenger();
        messenger.unregisterOutgoingPluginChannel(plugin, CHANNEL);
        messenger.unregisterIncomingPluginChannel(plugin, CHANNEL, this);
        registered = false;
    }

    String currentServer() {
        return currentServer;
    }

    /** Asks the proxy for this server's name; used to avoid reconnecting a player to where they already are. */
    void requestCurrentServer(Player player) {
        send(player, "GetServer", null);
    }

    void connect(Player player, PluginConfig.ServerEntry entry) {
        Messages messages = plugin.settings().messages();
        String display = Text.plain(entry.name());

        if (!Perms.has(player, entry.permission())) {
            messages.send(player, "server-no-permission", "%server%", display, "%id%", entry.id());
            return;
        }

        String current = currentServer;
        if (plugin.settings().preventConnectToCurrent() && current != null && current.equalsIgnoreCase(entry.id())) {
            messages.send(player, "already-connected", "%server%", display, "%id%", entry.id());
            return;
        }

        if (send(player, "Connect", entry.id())) {
            messages.send(player, "connecting", "%server%", display, "%id%", entry.id());
        } else {
            messages.send(player, "connect-failed", "%server%", display, "%id%", entry.id());
        }
    }

    private boolean send(Player player, String subChannel, String argument) {
        if (!registered || !plugin.isEnabled()) {
            return false;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(subChannel);
            if (argument != null) {
                out.writeUTF(argument);
            }
        } catch (IOException impossibleOnByteArray) {
            plugin.getLogger().log(Level.WARNING, "Could not build the '" + subChannel + "' proxy message",
                    impossibleOnByteArray);
            return false;
        }

        try {
            player.sendPluginMessage(plugin, CHANNEL, bytes.toByteArray());
            return true;
        } catch (RuntimeException channelUnavailable) {
            plugin.getLogger().log(Level.WARNING, "Could not send the '" + subChannel
                    + "' proxy message for " + player.getName(), channelUnavailable);
            return false;
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            if ("GetServer".equals(in.readUTF())) {
                currentServer = in.readUTF();
            }
        } catch (IOException | RuntimeException malformed) {
            // Another plugin's traffic on the shared channel, nothing to do.
        }
    }
}
