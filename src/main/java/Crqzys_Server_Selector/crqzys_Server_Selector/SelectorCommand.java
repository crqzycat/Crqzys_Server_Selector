package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The configurable menu command.
 * <p>
 * It extends {@link Command} and is registered against the server command map at runtime, because the
 * label lives in {@code config.yml} and therefore cannot be declared in {@code plugin.yml}.
 */
final class SelectorCommand extends Command {

    private final Crqzys_Server_Selector plugin;

    SelectorCommand(Crqzys_Server_Selector plugin, PluginConfig.CommandSettings settings, List<String> aliases) {
        // The command map may drop conflicting aliases straight out of this list, so it must be mutable.
        super(settings.name(), settings.description(), settings.usage(), new ArrayList<>(aliases));
        this.plugin = plugin;
        refreshPermission();
    }

    /** Re-reads the permission node, which a config reload may have changed. */
    void refreshPermission() {
        String permission = plugin.settings().permissions().open();
        setPermission(permission == null || permission.isBlank() ? null : permission);
    }

    @Override
    public boolean testPermission(CommandSender target) {
        if (testPermissionSilent(target)) {
            return true;
        }
        plugin.settings().messages().send(target, "no-permission");
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!plugin.isEnabled()) {
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            reload(sender);
            return true;
        }

        if (!testPermission(sender)) {
            return true;
        }

        if (!(sender instanceof Player player)) {
            plugin.settings().messages().send(sender, "players-only");
            return true;
        }

        SelectorMenu.open(plugin, player);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        if (!Perms.has(sender, plugin.settings().permissions().reload())) {
            return List.of();
        }
        return "reload".startsWith(args[0].toLowerCase(Locale.ROOT)) ? List.of("reload") : List.of();
    }

    private void reload(CommandSender sender) {
        Messages messages = plugin.settings().messages();
        if (!Perms.has(sender, plugin.settings().permissions().reload())) {
            messages.send(sender, "no-permission");
            return;
        }

        if (!plugin.reload()) {
            messages.send(sender, "reload-failed");
            return;
        }

        // Read the messages again: the reload may have replaced them.
        plugin.settings().messages().send(sender, "reloaded");

        String configuredName = plugin.settings().command().name();
        if (!configuredName.equals(getName())) {
            sender.sendMessage(Text.parse("&e[Selector] command.name is now '" + configuredName
                    + "' but this server is still serving '/" + getName()
                    + "'. Restart the server to apply the new command name."));
        }
    }
}
