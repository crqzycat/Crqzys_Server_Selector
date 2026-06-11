package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SelectorCommand implements CommandExecutor {

    private final Crqzys_Server_Selector plugin;

    public SelectorCommand(Crqzys_Server_Selector plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("serverselector.reload")) {
                player.sendMessage(ColorUtil.color("&cNo permission."));
                return true;
            }
            plugin.reloadConfig();
            player.sendMessage(ColorUtil.color("&aConfig reloaded."));
            return true;
        }

        SelectorGUI.open(player, plugin);
        return true;
    }
}
