package ua.kidlearn.themes;

import java.util.Map;

/** What a ThemeGenerator returns; ThemeValidator decides whether it becomes a draft or is rejected. */
public record GeneratedTheme(String name, Map<String, String> palette, String guideAccessory, String model) {
}
