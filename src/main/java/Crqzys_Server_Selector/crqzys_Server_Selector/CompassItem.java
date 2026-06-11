package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CompassItem {

    public static ItemStack build(Crqzys_Server_Selector plugin) {
        String materialName = plugin.getConfig().getString("compass.material", "COMPASS");
        Material material = Material.matchMaterial(materialName);
        if (material == null) material = Material.COMPASS;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ColorUtil.color(plugin.getConfig().getString("compass.name", "&aServer Selector")));

        List<String> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("compass.lore")) {
            lore.add(ColorUtil.color(line));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }
}
