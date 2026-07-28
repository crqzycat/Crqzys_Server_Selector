package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Declares the configured permission nodes to Bukkit.
 * <p>
 * Registering the nodes is what makes {@code default: true} work: an unregistered node is denied for
 * everyone but operators. It also makes the nodes show up in LuckPerms suggestions. No LuckPerms API
 * is needed beyond that, because every check goes through {@link CommandSender#hasPermission(String)}
 * which LuckPerms already answers.
 */
final class Perms {

    private final Plugin plugin;
    private final Set<String> registered = new LinkedHashSet<>();

    Perms(Plugin plugin) {
        this.plugin = plugin;
    }

    /** An empty node means "no permission required". */
    static boolean has(CommandSender sender, String node) {
        return node == null || node.isBlank() || sender.hasPermission(node);
    }

    void apply(PluginConfig config) {
        unregisterAll();

        PluginConfig.PermissionSettings settings = config.permissions();
        register(settings.open(), settings.openDefault(), "Allows opening the server selector menu.");
        register(settings.reload(), settings.reloadDefault(), "Allows reloading the server selector config.");
        register(settings.item(), settings.itemDefault(), "Allows receiving the server selector item.");
        for (PluginConfig.ServerEntry entry : config.servers()) {
            register(entry.permission(), settings.serverDefault(),
                    "Allows connecting to the '" + entry.id() + "' server.");
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            player.recalculatePermissions();
        }
    }

    void unregisterAll() {
        PluginManager manager = plugin.getServer().getPluginManager();
        for (String node : registered) {
            manager.removePermission(node);
        }
        registered.clear();
    }

    private void register(String node, PermissionDefault permissionDefault, String description) {
        if (node == null || node.isBlank()) {
            return;
        }

        PluginManager manager = plugin.getServer().getPluginManager();
        Permission existing = manager.getPermission(node);
        if (existing != null) {
            // Another plugin already owns the node; only align the default so the config stays authoritative.
            existing.setDefault(permissionDefault);
            manager.recalculatePermissionDefaults(existing);
            return;
        }

        Permission permission = new Permission(node, description, permissionDefault);
        manager.addPermission(permission);
        manager.recalculatePermissionDefaults(permission);
        registered.add(node);
    }
}
