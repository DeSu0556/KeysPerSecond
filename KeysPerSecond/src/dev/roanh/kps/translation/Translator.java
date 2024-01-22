package dev.roanh.kps.translation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class Translator {
    private static ResourceBundle resourceBundle;

    public static void loadTranslation(Locale locale) {
        resourceBundle = ResourceBundle.getBundle("translation", locale);
    }

    public static String translate(String key) {
        return resourceBundle.getString(key);
    }

    public static final Map<String, String> getAvailableLanguageMaps() {
        HashMap<String, String> map = new HashMap<>();
        map.put("English", "en");
        map.put("中文", "zh-CN");
        return map;
    }
}
