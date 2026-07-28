package Crqzys_Server_Selector.crqzys_Server_Selector;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Hands out the selector item and opens the menu when it is used. */
final class PlayerListener implements Listener {

    /** Guards against the menu being opened twice by a single click. */
    private static final long OPEN_COOLDOWN_MILLIS = 250L;

    private final Crqzys_Server_Selector plugin;
    private final Map<UUID, Long> lastOpen = new ConcurrentHashMap<>();

    PlayerListener(Crqzys_Server_Selector plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateItem(player, plugin.settings().item().giveOnJoin());
        if (plugin.proxy().currentServer() == null) {
            plugin.proxy().requestCurrentServer(player);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        updateItem(event.getPlayer(), plugin.settings().item().giveOnRespawn());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastOpen.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        // Only one of the two hands should react, otherwise the menu opens and instantly reopens.
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) {
            return;
        }
        if (!matchesConfiguredAction(event.getAction())) {
            return;
        }
        if (!SelectorItem.matches(plugin, event.getItem())) {
            return;
        }

        // Cancelled so the item cannot place blocks, break blocks or trigger the block it was aimed at.
        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!withinCooldown(player)) {
            return;
        }
        if (!Perms.has(player, plugin.settings().permissions().open())) {
            plugin.settings().messages().send(player, "no-permission");
            return;
        }

        SelectorMenu.open(plugin, player);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.settings().item().droppable()) {
            return;
        }
        if (SelectorItem.matches(plugin, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (plugin.settings().item().droppable()) {
            return;
        }
        // Otherwise the item is left on the ground for anyone to pick up, and the player gets a second one.
        Iterator<ItemStack> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            if (SelectorItem.matches(plugin, drops.next())) {
                drops.remove();
            }
        }
    }

    private boolean matchesConfiguredAction(Action action) {
        return switch (plugin.settings().item().openAction()) {
            case RIGHT_CLICK -> action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
            case LEFT_CLICK -> action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
            case ANY -> action != Action.PHYSICAL;
        };
    }

    private boolean withinCooldown(Player player) {
        long now = System.currentTimeMillis();
        Long previous = lastOpen.put(player.getUniqueId(), now);
        return previous == null || now - previous > OPEN_COOLDOWN_MILLIS;
    }

    /**
     * @param handOutIfMissing whether a player who carries no selector item should be given one; an item
     *                         the player already carries is refreshed either way
     */
    void updateItem(Player player, boolean handOutIfMissing) {
        PluginConfig.ItemSettings settings = plugin.settings().item();
        if (!settings.enabled() || !Perms.has(player, plugin.settings().permissions().item())) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        List<Integer> existing = findExisting(inventory);

        if (existing.isEmpty()) {
            if (handOutIfMissing) {
                place(player, SelectorItem.build(plugin), settings.slot());
            }
            return;
        }

        // Rebuilt rather than left alone: the config may have changed its name, lore or material since
        // it was handed out, and players keep the item in their inventory across restarts.
        ItemStack item = SelectorItem.build(plugin);
        for (int slot : existing) {
            inventory.setItem(slot, item);
        }

        int wanted = settings.slot();
        if (!settings.forceSlot() || existing.contains(wanted)) {
            return;
        }
        inventory.setItem(existing.get(0), null);
        place(player, item, wanted);
    }

    private void place(Player player, ItemStack item, int wanted) {
        PlayerInventory inventory = player.getInventory();
        ItemStack occupant = inventory.getItem(wanted);

        if (occupant == null || occupant.getType() == Material.AIR) {
            inventory.setItem(wanted, item);
            return;
        }
        if (!plugin.settings().item().forceSlot()) {
            giveOrDrop(player, item);
            return;
        }

        inventory.setItem(wanted, item);
        giveOrDrop(player, occupant);
    }

    /** Every slot holding a selector item; more than one only happens with items from older builds. */
    private List<Integer> findExisting(PlayerInventory inventory) {
        List<Integer> slots = new ArrayList<>(1);
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (SelectorItem.matches(plugin, inventory.getItem(slot))) {
                slots.add(slot);
            }
        }
        return slots;
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        for (ItemStack leftover : player.getInventory().addItem(stack).values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }
}
