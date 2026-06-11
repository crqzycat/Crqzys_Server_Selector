package Crqzys_Server_Selector.crqzys_Server_Selector;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;

public class BungeeUtil {

    public static void sendToServer(Player player, String serverId) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverId);
        player.sendPluginMessage(Crqzys_Server_Selector.getInstance(), "BungeeCord", out.toByteArray());
    }
}
