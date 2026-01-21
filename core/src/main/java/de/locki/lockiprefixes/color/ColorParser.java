package de.locki.lockiprefixes.color;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and translates color codes.
 * Supports:
 * - Legacy codes: &a, &b, &c, etc.
 * - Hex codes: &#RRGGBB, <#RRGGBB>
 * - RGB format: &x&R&R&G&G&B&B
 */
public class ColorParser {

    // Pattern for &#RRGGBB format
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    
    // Pattern for <#RRGGBB> format
    private static final Pattern HEX_BRACKET_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    
    // Pattern for legacy color codes
    private static final Pattern LEGACY_PATTERN = Pattern.compile("&([0-9A-Fa-fK-Ok-oRr])");

    /**
     * Translates all color codes to Minecraft format (§).
     * For versions that support hex colors (1.16+).
     *
     * @param text The text with color codes
     * @return The translated text
     */
    public static String translateHex(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // Convert <#RRGGBB> to &#RRGGBB first
        Matcher bracketMatcher = HEX_BRACKET_PATTERN.matcher(result);
        result = bracketMatcher.replaceAll("&#$1");

        // Convert &#RRGGBB to §x§R§R§G§G§B§B format
        Matcher hexMatcher = HEX_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append("§").append(c);
            }
            hexMatcher.appendReplacement(sb, replacement.toString());
        }
        hexMatcher.appendTail(sb);
        result = sb.toString();

        // Convert legacy &x codes to §x
        result = translateLegacy(result);

        return result;
    }

    /**
     * Translates only legacy color codes (&x -> §x).
     * For versions that don't support hex colors (pre-1.16).
     *
     * @param text The text with color codes
     * @return The translated text
     */
    public static String translateLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        Matcher matcher = LEGACY_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "§" + matcher.group(1));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Strips hex color codes from text, leaving only legacy codes.
     * Used for versions that don't support hex colors.
     *
     * @param text The text with color codes
     * @return The text with hex codes removed
     */
    public static String stripHex(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        // Remove <#RRGGBB> format
        result = HEX_BRACKET_PATTERN.matcher(result).replaceAll("");

        // Remove &#RRGGBB format
        result = HEX_PATTERN.matcher(result).replaceAll("");

        // Remove §x§R§R§G§G§B§B format
        result = result.replaceAll("§x(§[A-Fa-f0-9]){6}", "");

        return result;
    }

    /**
     * Strips all color codes from text.
     *
     * @param text The text with color codes
     * @return The plain text without color codes
     */
    public static String stripAll(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = stripHex(text);
        result = result.replaceAll("[§&][0-9A-Fa-fK-Ok-oRrXx]", "");
        return result;
    }
}
