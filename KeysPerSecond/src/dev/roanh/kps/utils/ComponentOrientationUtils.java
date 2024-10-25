package dev.roanh.kps.utils;

import java.awt.*;

public class ComponentOrientationUtils {
    /**
     * Recursively set the text direction of all components
     *
     * @param component   Specifying Component
     * @param orientation Specify text direction
     */
    public static void setTextOrientationRecursion(Component component, ComponentOrientation orientation) {
        component.setComponentOrientation(orientation);
        component.applyComponentOrientation(orientation);

        if (!(component instanceof Container)) {
            return;
        }

        for (Component subComponent : ((Container) component).getComponents()) {
            setTextOrientationRecursion(subComponent, orientation);
        }
    }
}
