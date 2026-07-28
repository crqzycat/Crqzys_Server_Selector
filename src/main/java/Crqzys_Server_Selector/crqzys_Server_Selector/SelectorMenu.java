package Crqzys_Server_Selector.crqzys_Server_Selector;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The selector inventory.
 * <p>
 * Being the inventory holder is how clicks are recognised. Comparing the window title, as the previous
 * version did, breaks as soon as a title contains colours the client normalises differently, and the
 * legacy {@code InventoryView#getTitle()} it relied on is deprecated.
 */
final class SelectorMenu implements InventoryHolder {

    private final Map<Integer, PluginConfig.ServerEntry> entriesBySlot = new HashMap<>();
    private final Inventory inventory;

    private SelectorMenu(Crqzys_Server_Selector plugin, Player viewer) {
        PluginConfig.GuiSettings gui = plugin.settings().gui();
        this.inventory = plugin.getServer().createInventory(this, gui.size(), gui.title());

        if (gui.fillerEnabled() && !gui.fillerMaterial().isAir()) {
            ItemStack filler = new ItemStack(gui.fillerMaterial());
            filler.editMeta(meta -> {
                meta.displayName(Component.empty());
                meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            });
            for (int slot = 0; slot < gui.size(); slot++) {
                inventory.setItem(slot, filler);
            }
        }

        for (PluginConfig.ServerEntry entry : plugin.settings().servers()) {
            boolean allowed = Perms.has(viewer, entry.permission());
            if (!allowed && entry.hideWithoutPermission()) {
                continue;
            }
            inventory.setItem(entry.slot(), icon(plugin, entry, allowed));
            entriesBySlot.put(entry.slot(), entry);
        }
    }

    static void open(Crqzys_Server_Selector plugin, Player viewer) {
        if (plugin.settings().servers().isEmpty()) {
            plugin.settings().messages().send(viewer, "no-servers");
            return;
        }
        viewer.openInventory(new SelectorMenu(plugin, viewer).getInventory());
    }

    PluginConfig.ServerEntry entryAt(int slot) {
        return entriesBySlot.get(slot);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private static ItemStack icon(Crqzys_Server_Selector plugin, PluginConfig.ServerEntry entry, boolean allowed) {
        List<Component> lore = new ArrayList<>(entry.lore());
        if (!allowed) {
            Component locked = plugin.settings().messages().get("locked-lore");
            if (!Text.plain(locked).isEmpty()) {
                lore.add(Text.item(locked));
            }
        }

        ItemStack stack = new ItemStack(entry.material());
        stack.editMeta(meta -> {
            meta.displayName(entry.name());
            if (!lore.isEmpty()) {
                meta.lore(lore);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return stack;
    }
}
