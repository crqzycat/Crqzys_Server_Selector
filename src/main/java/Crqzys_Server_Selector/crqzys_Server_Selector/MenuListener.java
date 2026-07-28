package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Handles clicks inside the menu, and keeps the selector item from being moved or lost. */
final class MenuListener implements Listener {

    private final Crqzys_Server_Selector plugin;

    MenuListener(Crqzys_Server_Selector plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();

        if (top.getHolder() instanceof SelectorMenu menu) {
            // Nothing in this window may ever be taken, including via shift-click from the bottom inventory.
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }
            if (event.getClickedInventory() != top) {
                return;
            }

            PluginConfig.ServerEntry entry = menu.entryAt(event.getRawSlot());
            if (entry == null) {
                return;
            }
            if (plugin.settings().gui().closeOnClick()) {
                player.closeInventory();
            }
            plugin.proxy().connect(player, entry);
            return;
        }

        if (!plugin.settings().item().locked()) {
            return;
        }
        if (SelectorItem.matches(plugin, event.getCurrentItem())
                || SelectorItem.matches(plugin, event.getCursor())
                || isHotbarSwap(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SelectorMenu) {
            event.setCancelled(true);
            return;
        }
        if (plugin.settings().item().locked() && SelectorItem.matches(plugin, event.getOldCursor())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        if (!plugin.settings().item().locked()) {
            return;
        }
        if (SelectorItem.matches(plugin, event.getMainHandItem())
                || SelectorItem.matches(plugin, event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    /** Right-clicking an item frame hands the item over to it, which no inventory event would catch. */
    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!plugin.settings().item().locked()) {
            return;
        }
        ItemStack used = event.getPlayer().getInventory().getItem(event.getHand());
        if (SelectorItem.matches(plugin, used)) {
            event.setCancelled(true);
        }
    }

    /** Only reachable when the item material is edible, but then the item would simply be eaten away. */
    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!plugin.settings().item().locked()) {
            return;
        }
        if (SelectorItem.matches(plugin, event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Number keys move an item without it ever being the clicked item or the cursor. */
    private boolean isHotbarSwap(InventoryClickEvent event) {
        if (event.getClick() != ClickType.NUMBER_KEY || !(event.getWhoClicked() instanceof Player player)) {
            return false;
        }
        int hotbarSlot = event.getHotbarButton();
        if (hotbarSlot < 0) {
            return false;
        }
        ItemStack hotbarItem = player.getInventory().getItem(hotbarSlot);
        return SelectorItem.matches(plugin, hotbarItem);
    }
}
