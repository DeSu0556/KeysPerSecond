package dev.roanh.kps.translation;

import java.awt.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class Translator {
    private static ResourceBundle defaultResourceBundle;
    private static ResourceBundle resourceBundle;

    /**
     * Get the text orientation of the current translation
     */
    public static ComponentOrientation getCurrentTextOrientation() {
        return ComponentOrientation.getOrientation(resourceBundle.getLocale());
    }

    /**
     * Load default translation. Default is English.
     */
    public static void loadDefaultTranslation() {
        defaultResourceBundle = ResourceBundle.getBundle("translation", Locale.ENGLISH);
    }

    /**
     * Load the specified translation file
     *
     * @param locale Specify the language
     */
    public static void loadTranslation(Locale locale) {
        if (locale == Locale.ENGLISH) {
            resourceBundle = defaultResourceBundle;
        } else {
            resourceBundle = ResourceBundle.getBundle("translation", locale);
        }
    }

    /**
     * Get the corresponding translation by key
     *
     * @param key Specify the key
     * @return If there is no translation result, use English instead.
     */
    public static String translate(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (Exception e) {
            System.out.println("Cant find translation (" + key + ") in " + resourceBundle.getLocale().getDisplayName() + ", Use English instead");
            return defaultResourceBundle.getString(key);
        }
    }

    /**
     * Get the corresponding translation through key and execute format
     *
     * @param key    Specify the key
     * @param values Specify the format parameter
     * @return If there is no translation result, use English instead.
     */
    public static final String translate(String key, Object... values) {
        String resultWithoutFormat = translate(key);
        return String.format(resultWithoutFormat, values);
    }

    /**
     * Get the current language
     *
     * @return Current language
     */
    public static Locale getCurrentUsingLocale() {
        return resourceBundle.getLocale();
    }

    /**
     * Get a list of available languages
     */
    public static final Map<String, String> getAvailableLanguageMaps() {
        HashMap<String, String> map = new HashMap<>();
        map.put("en", "English");
        map.put("zh_CN", "简体中文");
        map.put("ar_SA", "عربي");
        return map;
    }
}
