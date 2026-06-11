package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SelectorGUI {

    public static void open(Player player, Crqzys_Server_Selector plugin) {
        String title = ColorUtil.color(plugin.getConfig().getString("gui.title", "&8Server Selector"));
        int size = plugin.getConfig().getInt("gui.size", 27);
        // Clamp to valid chest sizes (multiples of 9, max 54)
        if (size % 9 != 0 || size < 9 || size > 54) size = 27;

        Inventory gui = Bukkit.createInventory(null, size, title);

        // Fill with glass panes if enabled
        if (plugin.getConfig().getBoolean("gui.fill-border", true)) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta fm = filler.getItemMeta();
            fm.setDisplayName(" ");
            filler.setItemMeta(fm);
            for (int i = 0; i < size; i++) gui.setItem(i, filler);
        }

        var servers = plugin.getConfig().getConfigurationSection("servers");
        if (servers == null) {
            player.openInventory(gui);
            return;
        }

        for (String key : servers.getKeys(false)) {
            String materialName = plugin.getConfig().getString("servers." + key + ".material", "PAPER");
            Material material = Material.matchMaterial(materialName);
            if (material == null) material = Material.PAPER;

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ColorUtil.color(plugin.getConfig().getString("servers." + key + ".name", key)));

            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("servers." + key + ".lore")) {
                lore.add(ColorUtil.color(line));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);

            int slot = plugin.getConfig().getInt("servers." + key + ".slot", gui.firstEmpty());
            if (slot >= 0 && slot < size) {
                gui.setItem(slot, item);
            }
        }

        player.openInventory(gui);
    }
}
