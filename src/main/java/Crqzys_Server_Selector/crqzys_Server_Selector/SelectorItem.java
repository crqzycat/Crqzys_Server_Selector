package Crqzys_Server_Selector.crqzys_Server_Selector;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Builds and recognises the item that opens the menu.
 * <p>
 * Recognition is based on a persistent data tag instead of the display name, so renaming the item in
 * the config does not orphan the items players already carry, and a player cannot craft a fake one by
 * renaming a compass in an anvil.
 */
final class SelectorItem {

    private static final byte MARKER = 1;

    private SelectorItem() {
    }

    static ItemStack build(Crqzys_Server_Selector plugin) {
        PluginConfig.ItemSettings settings = plugin.settings().item();
        ItemStack stack = new ItemStack(settings.material());
        stack.editMeta(meta -> {
            meta.displayName(settings.name());
            if (!settings.lore().isEmpty()) {
                meta.lore(settings.lore());
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.getPersistentDataContainer().set(plugin.itemKey(), PersistentDataType.BYTE, MARKER);
        });
        return stack;
    }

    static boolean matches(Crqzys_Server_Selector plugin, ItemStack stack) {
        if (stack == null || stack.getType().isAir() || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (meta.getPersistentDataContainer().has(plugin.itemKey(), PersistentDataType.BYTE)) {
            return true;
        }

        // Items handed out by older versions of the plugin carry no tag; fall back to name matching so
        // they keep working until the player is given a fresh one.
        PluginConfig.ItemSettings settings = plugin.settings().item();
        if (stack.getType() != settings.material()) {
            return false;
        }
        Component displayName = meta.displayName();
        return displayName != null && !Text.plain(settings.name()).isEmpty()
                && Text.plain(displayName).equals(Text.plain(settings.name()));
    }
}
