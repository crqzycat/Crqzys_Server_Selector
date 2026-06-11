package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private final Crqzys_Server_Selector plugin;

    public PlayerListener(Crqzys_Server_Selector plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        giveCompassIfMissing(player);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        String compassMaterial = plugin.getConfig().getString("compass.material", "COMPASS");
        if (!item.getType().name().equals(compassMaterial)) return;

        // Check if it's the selector compass by display name
        if (!item.hasItemMeta()) return;
        String expectedName = plugin.getConfig().getString("compass.name", "&aServer Selector");
        String displayName = item.getItemMeta().getDisplayName();
        if (!displayName.equals(ColorUtil.color(expectedName))) return;

        event.setCancelled(true);
        SelectorGUI.open(player, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = plugin.getConfig().getString("gui.title", "&8Server Selector");
        if (!event.getView().getTitle().equals(ColorUtil.color(title))) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!clicked.hasItemMeta() || !clicked.getItemMeta().hasDisplayName()) return;

        // Find which server was clicked by matching display name
        var servers = plugin.getConfig().getConfigurationSection("servers");
        if (servers == null) return;

        for (String key : servers.getKeys(false)) {
            String serverName = ColorUtil.color(plugin.getConfig().getString("servers." + key + ".name", key));
            if (clicked.getItemMeta().getDisplayName().equals(serverName)) {
                String serverId = plugin.getConfig().getString("servers." + key + ".id", key);
                BungeeUtil.sendToServer(player, serverId);
                player.closeInventory();
                return;
            }
        }
    }

    private void giveCompassIfMissing(Player player) {
        String materialName = plugin.getConfig().getString("compass.material", "COMPASS");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.COMPASS;

        // Check if player already has the selector compass
        String expectedName = ColorUtil.color(plugin.getConfig().getString("compass.name", "&aServer Selector"));
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (item.getType() != material) continue;
            if (!item.hasItemMeta()) continue;
            if (expectedName.equals(item.getItemMeta().getDisplayName())) return; // already has it
        }

        player.getInventory().addItem(CompassItem.build(plugin));
    }
}
