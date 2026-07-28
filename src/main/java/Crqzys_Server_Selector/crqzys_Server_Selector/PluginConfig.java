package Crqzys_Server_Selector.crqzys_Server_Selector;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Immutable snapshot of {@code config.yml}.
 * <p>
 * Everything is parsed and validated once per (re)load so the event handlers never have to guess
 * what a malformed value means, and so a config edit can never leave the plugin half updated.
 */
final class PluginConfig {

    static final int CURRENT_VERSION = 2;

    enum OpenAction {
        RIGHT_CLICK,
        LEFT_CLICK,
        ANY
    }

    record CommandSettings(String name, List<String> aliases, String description, String usage) {
    }

    record PermissionSettings(String open, PermissionDefault openDefault,
                              String reload, PermissionDefault reloadDefault,
                              String item, PermissionDefault itemDefault,
                              String serverPrefix, PermissionDefault serverDefault) {
    }

    record ItemSettings(boolean enabled, Material material, Component name, List<Component> lore,
                        int slot, boolean forceSlot, boolean droppable, boolean locked,
                        boolean giveOnJoin, boolean giveOnRespawn, OpenAction openAction) {
    }

    record GuiSettings(Component title, int size, boolean fillerEnabled, Material fillerMaterial,
                       boolean closeOnClick) {
    }

    record ServerEntry(String key, String id, Material material, Component name, List<Component> lore,
                       int slot, String permission, boolean hideWithoutPermission) {
    }

    private final CommandSettings command;
    private final PermissionSettings permissions;
    private final ItemSettings item;
    private final GuiSettings gui;
    private final List<ServerEntry> servers;
    private final Messages messages;
    private final boolean preventConnectToCurrent;

    private PluginConfig(CommandSettings command, PermissionSettings permissions, ItemSettings item,
                         GuiSettings gui, List<ServerEntry> servers, Messages messages,
                         boolean preventConnectToCurrent) {
        this.command = command;
        this.permissions = permissions;
        this.item = item;
        this.gui = gui;
        this.servers = List.copyOf(servers);
        this.messages = messages;
        this.preventConnectToCurrent = preventConnectToCurrent;
    }

    CommandSettings command() {
        return command;
    }

    PermissionSettings permissions() {
        return permissions;
    }

    ItemSettings item() {
        return item;
    }

    GuiSettings gui() {
        return gui;
    }

    List<ServerEntry> servers() {
        return servers;
    }

    Messages messages() {
        return messages;
    }

    boolean preventConnectToCurrent() {
        return preventConnectToCurrent;
    }

    static PluginConfig load(JavaPlugin plugin) {
        FileConfiguration config = plugin.getConfig();
        Logger log = plugin.getLogger();

        int version = config.getInt("config-version", 1);
        if (version < CURRENT_VERSION) {
            log.warning("config.yml was written for an older version of the plugin (found "
                    + version + ", expected " + CURRENT_VERSION + "). Missing options fall back to their "
                    + "defaults; delete the file to regenerate a fully documented one.");
        }

        GuiSettings gui = loadGui(config, log);

        return new PluginConfig(
                loadCommand(config, log),
                loadPermissions(config, log),
                loadItem(config, log),
                gui,
                loadServers(config, gui.size(), log),
                new Messages(config.getConfigurationSection("messages")),
                config.getBoolean("prevent-connect-to-current", true));
    }

    private static CommandSettings loadCommand(FileConfiguration config, Logger log) {
        String name = sanitizeLabel(config.getString("command.name", "selector"));
        if (name.isEmpty()) {
            log.warning("command.name is empty or invalid, falling back to 'selector'.");
            name = "selector";
        }

        List<String> aliases = new ArrayList<>();
        for (String alias : config.getStringList("command.aliases")) {
            String sanitized = sanitizeLabel(alias);
            if (sanitized.isEmpty() || sanitized.equals(name) || aliases.contains(sanitized)) {
                continue;
            }
            aliases.add(sanitized);
        }

        return new CommandSettings(
                name,
                List.copyOf(aliases),
                config.getString("command.description", "Open the server selector"),
                config.getString("command.usage", "/<command> [reload]"));
    }

    private static PermissionSettings loadPermissions(FileConfiguration config, Logger log) {
        return new PermissionSettings(
                node(config, "permissions.open", "serverselector.use"),
                permissionDefault(config, "permissions.open-default", PermissionDefault.TRUE, log),
                node(config, "permissions.reload", "serverselector.reload"),
                permissionDefault(config, "permissions.reload-default", PermissionDefault.OP, log),
                node(config, "permissions.item", "serverselector.item"),
                permissionDefault(config, "permissions.item-default", PermissionDefault.TRUE, log),
                node(config, "permissions.server-prefix", "serverselector.server."),
                permissionDefault(config, "permissions.server-default", PermissionDefault.TRUE, log));
    }

    private static ItemSettings loadItem(FileConfiguration config, Logger log) {
        int slot = config.getInt("compass.slot", 4);
        if (slot < 0 || slot > 35) {
            log.warning("compass.slot must be between 0 and 35 (0-8 is the hotbar), got " + slot
                    + ", using 4 instead.");
            slot = 4;
        }

        String rawAction = config.getString("compass.open-on", "RIGHT_CLICK");
        OpenAction openAction;
        try {
            openAction = OpenAction.valueOf(rawAction.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException invalid) {
            log.warning("compass.open-on must be RIGHT_CLICK, LEFT_CLICK or ANY, got '" + rawAction
                    + "', using RIGHT_CLICK instead.");
            openAction = OpenAction.RIGHT_CLICK;
        }

        return new ItemSettings(
                config.getBoolean("compass.enabled", true),
                material(config, "compass.material", Material.COMPASS, log),
                Text.item(config.getString("compass.name", "&6&lServer Selector")),
                lore(config, "compass.lore"),
                slot,
                config.getBoolean("compass.force-slot", true),
                config.getBoolean("compass.droppable", false),
                config.getBoolean("compass.locked", true),
                config.getBoolean("compass.give-on-join", true),
                config.getBoolean("compass.give-on-respawn", true),
                openAction);
    }

    private static GuiSettings loadGui(FileConfiguration config, Logger log) {
        int size = config.getInt("gui.size", 27);
        if (size < 9 || size > 54 || size % 9 != 0) {
            log.warning("gui.size must be a multiple of 9 between 9 and 54, got " + size
                    + ", using 27 instead.");
            size = 27;
        }

        // 'fill-border' is the name this option had in v1 configs.
        boolean fillerEnabled = config.getBoolean("gui.filler.enabled", config.getBoolean("gui.fill-border", true));

        return new GuiSettings(
                Text.parse(config.getString("gui.title", "&8&lServer Selector")),
                size,
                fillerEnabled,
                material(config, "gui.filler.material", Material.GRAY_STAINED_GLASS_PANE, log),
                config.getBoolean("gui.close-on-click", true));
    }

    private static List<ServerEntry> loadServers(FileConfiguration config, int guiSize, Logger log) {
        ConfigurationSection section = config.getConfigurationSection("servers");
        if (section == null || section.getKeys(false).isEmpty()) {
            log.warning("No servers are configured under 'servers', the menu will be empty.");
            return List.of();
        }

        String serverPrefix = node(config, "permissions.server-prefix", "serverselector.server.");

        List<ServerEntry> entries = new ArrayList<>();
        Map<Integer, String> usedSlots = new HashMap<>();

        for (String key : new LinkedHashSet<>(section.getKeys(false))) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                log.warning("servers." + key + " is not a section and was skipped.");
                continue;
            }

            String id = entry.getString("id", key);
            if (id == null || id.isBlank()) {
                log.warning("servers." + key + ".id is empty, using '" + key + "' instead.");
                id = key;
            }

            int slot = entry.getInt("slot", -1);
            if (slot < 0 || slot >= guiSize) {
                log.warning("servers." + key + ".slot must be between 0 and " + (guiSize - 1)
                        + " for a menu of size " + guiSize + ", got " + slot + ", entry skipped.");
                continue;
            }
            String conflicting = usedSlots.putIfAbsent(slot, key);
            if (conflicting != null) {
                log.warning("servers." + key + " uses slot " + slot + " which is already taken by servers."
                        + conflicting + ", entry skipped.");
                continue;
            }

            String permission;
            if (entry.isSet("permission")) {
                permission = trimToEmpty(entry.getString("permission"));
            } else {
                permission = serverPrefix.isEmpty() ? "" : serverPrefix + key.toLowerCase(Locale.ROOT);
            }

            entries.add(new ServerEntry(
                    key,
                    id.trim(),
                    material(entry, "material", Material.PAPER, log, "servers." + key + ".material"),
                    Text.item(entry.getString("name", key)),
                    lore(entry, "lore"),
                    slot,
                    permission,
                    entry.getBoolean("hide-without-permission", false)));
        }

        entries.sort((left, right) -> Integer.compare(left.slot(), right.slot()));
        return entries;
    }

    private static List<Component> lore(ConfigurationSection section, String path) {
        List<String> raw = section.getStringList(path);
        if (raw.isEmpty()) {
            return List.of();
        }
        List<Component> lore = new ArrayList<>(raw.size());
        for (String line : raw) {
            lore.add(Text.item(line));
        }
        return Collections.unmodifiableList(lore);
    }

    private static Material material(ConfigurationSection section, String path, Material fallback, Logger log) {
        return material(section, path, fallback, log, path);
    }

    private static Material material(ConfigurationSection section, String path, Material fallback,
                                     Logger log, String displayPath) {
        String raw = section.getString(path);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Material material = Material.matchMaterial(raw.trim());
        if (material == null) {
            log.warning(displayPath + ": '" + raw + "' is not a material known to this server version, "
                    + "using " + fallback + " instead.");
            return fallback;
        }
        if (!material.isItem()) {
            log.warning(displayPath + ": " + material + " cannot exist as an item, using " + fallback
                    + " instead.");
            return fallback;
        }
        return material;
    }

    private static PermissionDefault permissionDefault(FileConfiguration config, String path,
                                                       PermissionDefault fallback, Logger log) {
        String raw = config.getString(path);
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        PermissionDefault parsed = PermissionDefault.getByName(raw.trim());
        if (parsed == null) {
            log.warning(path + ": '" + raw + "' is not a valid permission default "
                    + "(true, false, op or not_op), using " + fallback.name().toLowerCase(Locale.ROOT) + ".");
            return fallback;
        }
        return parsed;
    }

    private static String node(FileConfiguration config, String path, String fallback) {
        if (!config.isSet(path)) {
            return fallback;
        }
        return trimToEmpty(config.getString(path));
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /** Command labels may not contain spaces or a leading slash. */
    private static String sanitizeLabel(String raw) {
        if (raw == null) {
            return "";
        }
        String label = raw.trim().toLowerCase(Locale.ROOT);
        while (label.startsWith("/")) {
            label = label.substring(1);
        }
        return label.replace(" ", "").replace(":", "");
    }
}
