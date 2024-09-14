package dev.roanh.kps.utils;

import java.util.Map;

public class MapUtils {
    /*
    * Find the specified Entry by Key
    * */
    public static <K, V> Map.Entry<K, V> getEntryByKey(Map<K, V> map, K key) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (entry.getKey().equals(key)) {
                return entry;
            }
        }
        return null;
    }
}
