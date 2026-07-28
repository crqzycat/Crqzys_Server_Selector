package Crqzys_Server_Selector.crqzys_Server_Selector;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Pattern;

/**
 * Turns configuration strings into components.
 * <p>
 * Legacy {@code &} codes and MiniMessage tags are both accepted so existing configs keep working.
 * Components are used everywhere instead of {@code ChatColor} because the legacy string based
 * item/inventory methods are deprecated and may disappear in future Minecraft releases.
 */
final class Text {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final Pattern LEGACY_CODE =
            Pattern.compile("&(?:[0-9a-fk-orA-FK-OR]|#[0-9a-fA-F]{6}|x(?:&[0-9a-fA-F]){6})");

    private Text() {
    }

    static Component parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        if (LEGACY_CODE.matcher(raw).find()) {
            return LEGACY.deserialize(raw);
        }
        try {
            return MINI.deserialize(raw);
        } catch (RuntimeException invalidTags) {
            return Component.text(raw);
        }
    }

    /** Same as {@link #parse}, minus the italics vanilla adds to renamed items. */
    static Component item(String raw) {
        return item(parse(raw));
    }

    static Component item(Component component) {
        return component
                .colorIfAbsent(NamedTextColor.WHITE)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    static String plain(Component component) {
        return component == null ? "" : PlainTextComponentSerializer.plainText().serialize(component);
    }
}
