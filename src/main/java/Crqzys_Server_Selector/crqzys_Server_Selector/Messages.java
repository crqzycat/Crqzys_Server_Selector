package Crqzys_Server_Selector.crqzys_Server_Selector;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashMap;
import java.util.Map;

/** All player facing text, resolved from the {@code messages} config section. */
final class Messages {

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("prefix", "&8[&6Selector&8]&r ");
        DEFAULTS.put("no-permission", "%prefix%&cYou are not allowed to do that.");
        DEFAULTS.put("players-only", "%prefix%&cOnly players can use this command.");
        DEFAULTS.put("reloaded", "%prefix%&aConfiguration reloaded.");
        DEFAULTS.put("reload-failed", "%prefix%&cReload failed, check the console for details.");
        DEFAULTS.put("connecting", "%prefix%&7Connecting you to &f%server%&7...");
        DEFAULTS.put("already-connected", "%prefix%&cYou are already connected to &f%server%&c.");
        DEFAULTS.put("server-no-permission", "%prefix%&cYou do not have access to &f%server%&c.");
        DEFAULTS.put("connect-failed", "%prefix%&cCould not reach the proxy, please try again later.");
        DEFAULTS.put("no-servers", "%prefix%&cNo servers are configured.");
        DEFAULTS.put("locked-lore", "&c&oLocked");
    }

    private final Map<String, String> values = new LinkedHashMap<>(DEFAULTS);

    Messages(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        for (String key : DEFAULTS.keySet()) {
            String raw = section.getString(key);
            if (raw != null) {
                values.put(key, raw);
            }
        }
    }

    /**
     * @param placeholders alternating placeholder/value pairs, e.g. {@code "%server%", "Lobby"}
     */
    Component get(String key, String... placeholders) {
        return Text.parse(raw(key, placeholders));
    }

    /** Sends the message, unless it was blanked out in the config. */
    void send(CommandSender target, String key, String... placeholders) {
        String raw = raw(key, placeholders);
        if (raw.isBlank()) {
            return;
        }
        target.sendMessage(Text.parse(raw));
    }

    private String raw(String key, String... placeholders) {
        String raw = values.getOrDefault(key, "");
        if (raw.isEmpty()) {
            return raw;
        }
        raw = raw.replace("%prefix%", values.getOrDefault("prefix", ""));
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace(placeholders[i], placeholders[i + 1]);
        }
        return raw;
    }
}
