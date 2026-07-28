package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public final class Crqzys_Server_Selector extends JavaPlugin {

    private NamespacedKey itemKey;
    private PluginConfig settings;
    private Perms permissions;
    private ProxyBridge proxy;
    private SelectorCommand command;
    private PlayerListener playerListener;

    @Override
    public void onEnable() {
        itemKey = new NamespacedKey(this, "selector_item");
        saveDefaultConfig();

        permissions = new Perms(this);
        if (!reload()) {
            getLogger().severe("config.yml could not be read, disabling the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        proxy = new ProxyBridge(this);
        proxy.register();

        playerListener = new PlayerListener(this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);

        registerCommand();

        // Players are already online when the plugin is enabled through a reload or a plugin manager.
        for (Player player : getServer().getOnlinePlayers()) {
            if (settings.item().giveOnJoin()) {
                playerListener.giveItem(player);
            }
            proxy.requestCurrentServer(player);
        }
    }

    @Override
    public void onDisable() {
        // Leaving the menu open would let players interact with an inventory nothing is listening to.
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof SelectorMenu) {
                player.closeInventory();
            }
        }

        unregisterCommand();

        if (proxy != null) {
            proxy.unregister();
        }
        if (permissions != null) {
            permissions.unregisterAll();
        }
    }

    /** @return {@code false} when the config could not be parsed, in which case the previous one stays active */
    boolean reload() {
        PluginConfig loaded;
        try {
            reloadConfig();
            loaded = PluginConfig.load(this);
        } catch (RuntimeException | LinkageError broken) {
            getLogger().log(Level.SEVERE, "Failed to load config.yml", broken);
            return settings != null;
        }

        settings = loaded;
        permissions.apply(settings);
        if (command != null) {
            command.refreshPermission();
            for (Player player : getServer().getOnlinePlayers()) {
                player.updateCommands();
            }
        }
        return true;
    }

    PluginConfig settings() {
        return settings;
    }

    ProxyBridge proxy() {
        return proxy;
    }

    NamespacedKey itemKey() {
        return itemKey;
    }

    private void registerCommand() {
        PluginConfig.CommandSettings settings = this.settings.command();
        CommandMap commandMap = getServer().getCommandMap();
        String fallbackPrefix = getName().toLowerCase(Locale.ROOT);

        // An alias that is already taken stays pointing at its original command, so keeping it would only
        // make the help output lie about what works.
        List<String> aliases = new ArrayList<>();
        for (String alias : settings.aliases()) {
            if (commandMap.getCommand(alias) != null) {
                getLogger().warning("Alias /" + alias + " is already used by another command and was skipped.");
                continue;
            }
            aliases.add(alias);
        }

        command = new SelectorCommand(this, settings, aliases);
        if (!commandMap.register(settings.name(), fallbackPrefix, command)) {
            getLogger().warning("/" + settings.name() + " is already taken by another plugin. Use /"
                    + fallbackPrefix + ":" + settings.name() + " instead, or pick another command.name.");
        }

        for (Player player : getServer().getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    private void unregisterCommand() {
        if (command == null) {
            return;
        }

        SelectorCommand registered = command;
        command = null;

        CommandMap commandMap = getServer().getCommandMap();
        registered.unregister(commandMap);

        // Command#unregister only clears the command's own registration state; the map keeps pointing at
        // it, which would leave a dead command behind if the plugin is enabled again in the same session.
        try {
            Method knownCommands = commandMap.getClass().getMethod("getKnownCommands");
            Object result = knownCommands.invoke(commandMap);
            if (result instanceof Map<?, ?> map) {
                map.values().removeIf(value -> value == registered);
            }
        } catch (ReflectiveOperationException | RuntimeException unsupported) {
            getLogger().fine("Could not detach /" + registered.getName() + " from the command map: " + unsupported);
        }
    }
}
