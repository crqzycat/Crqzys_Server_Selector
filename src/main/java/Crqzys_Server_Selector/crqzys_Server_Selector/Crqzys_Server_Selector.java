package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.plugin.java.JavaPlugin;

public final class Crqzys_Server_Selector extends JavaPlugin {

    private static Crqzys_Server_Selector instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // Register BungeeCord plugin messaging channel
        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getCommand("selector").setExecutor(new SelectorCommand(this));

        getLogger().info("ServerSelector enabled!");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        getLogger().info("ServerSelector disabled.");
    }

    public static Crqzys_Server_Selector getInstance() {
        return instance;
    }
}
