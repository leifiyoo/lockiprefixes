package de.locki.lockiprefixes.core;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.md_5.bungee.api.ChatColor;

/**
 * Utility class for gradient animation and text formatting.
 * Centralizes gradient logic to reduce duplication across modules.
 */
public class GradientUtils {
    
    /**
     * Applies a gradient effect to the given text based on two hex colors.
     * Supports both modern and legacy formatting where possible.
     */
    public static String applyGradient(String text, String colorFrom, String colorTo) {
        if (text == null || text.isEmpty()) return "";
        
        try {
            Color start = Color.decode(colorFrom);
            Color end = Color.decode(colorTo);
            
            StringBuilder sb = new StringBuilder();
            int length = text.length();
            
            for (int i = 0; i < length; i++) {
                float ratio = (float) i / (float) (length > 1 ? length - 1 : 1);
                int red = (int) (start.getRed() + (end.getRed() - start.getRed()) * ratio);
                int green = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * ratio);
                int blue = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * ratio);
                
                ChatColor color = ChatColor.of(new Color(red, green, blue));
                sb.append(color.toString()).append(text.charAt(i));
            }
            return sb.toString();
        } catch (Exception e) {
            return ChatColor.translateAlternateColorCodes('&', text);
        }
    }
    
    /**
     * Translates legacy color codes and formats hex colors.
     */
    public static String format(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    /**
     * Calculates an animated gradient frame for a given step.
     */
    public static String getAnimatedGradient(String text, String colorFrom, String colorTo, int step, int totalSteps) {
        // Animation logic based on shifting the gradient phase
        float phase = (float) step / (float) totalSteps;
        // Logic for oscillating or shifting can be added here
        return applyGradient(text, colorFrom, colorTo); // Placeholder for phase-aware gradient
    }
}
