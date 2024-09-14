package dev.roanh.kps.translation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class Translator {
    private static ResourceBundle defaultResourceBundle;
    private static ResourceBundle resourceBundle;

    public static void loadDefaultTranslation() {
        defaultResourceBundle = ResourceBundle.getBundle("translation", Locale.ENGLISH);
    }

    public static void loadTranslation(Locale locale) {
        if (locale == Locale.ENGLISH) {
            resourceBundle = defaultResourceBundle;
        } else {
            resourceBundle = ResourceBundle.getBundle("translation", locale);
        }
    }

    public static String translate(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Exception e) {
            System.out.println("Cant find translation (" + key + ") in " + resourceBundle.getLocale().getDisplayName() + ", Use English instead");
            return defaultResourceBundle.getString(key);
        }
    }

    public static final String translate(String key, Object... values) {
        String resultWithoutFormat = translate(key);
        return String.format(resultWithoutFormat, values);
    }

    public static Locale getCurrentUsingLocale() {
        return resourceBundle.getLocale();
    }

    public static final Map<String, String> getAvailableLanguageMaps() {
        HashMap<String, String> map = new HashMap<>();
        map.put("en", "English");
        map.put("zh_CN", "简体中文");
        map.put("ar_SA", "عربي");
        return map;
    }
}
