/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.ui.css.swt.internal.theme.BootstrapTheme3x;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.e4.ui.css.swt.theme.IThemeManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.IWorkbenchThemeConstants;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import java.util.Collection;

/**
 * UI Utils
 */
public class UIStyles {

    public static final Color COLOR_WHITE = new Color(null, 255, 255, 255);
    private static final Log log = Log.getLog(UIStyles.class);

    private static final String THEME_HIGH_CONTRAST_ID = "org.eclipse.e4.ui.css.theme.high-contrast";
    static final Color COLOR_BLACK = new Color(null, 0, 0, 0);
    static final Color COLOR_WHITE_DARK = new Color(null, 192, 192, 192);

    static IPreferenceStore EDITORS_PREFERENCE_STORE;
    
    static IThemeEngine themeEngine = null;

    public static synchronized IPreferenceStore getEditorsPreferenceStore() {
        if (EDITORS_PREFERENCE_STORE == null) {
            EDITORS_PREFERENCE_STORE = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.editors");
        }
        return EDITORS_PREFERENCE_STORE;
    }

    public static boolean isDarkTheme() {
        return UIUtils.isDark(getDefaultTextBackground().getRGB()) || isDarkHighContrastTheme();
    }

    private static IThemeEngine getThemeEngine() {
        if (themeEngine == null) {
            Bundle bundle = FrameworkUtil.getBundle(BootstrapTheme3x.class);
            if (bundle != null) {
                BundleContext context = bundle.getBundleContext();
                if (context != null) {
                    ServiceReference<IThemeManager> ref = context.getServiceReference(IThemeManager.class);
                    if (ref != null) {
                        IThemeManager manager = context.getService(ref);
                        if (manager != null) {
                            themeEngine = manager.getEngineForDisplay(Display.getDefault());
                        }
                    }
                }
            }
        }
        return themeEngine;
    }

    public static boolean isHighContrastTheme() {
        IThemeEngine themeEngine = getThemeEngine();
        org.eclipse.e4.ui.css.swt.theme.ITheme theme = null;
        if (themeEngine != null) {
            theme = themeEngine.getActiveTheme(); 
        } else {
            themeEngine = PlatformUI.getWorkbench().getService(IThemeEngine.class);
            if (themeEngine != null) {
                theme = themeEngine.getActiveTheme();
            }
        }
        if (theme != null) {
            return theme.getId().equals(THEME_HIGH_CONTRAST_ID);
        }
        return false;
    }
    
    public static boolean isDarkHighContrastTheme() {
        return isHighContrastTheme() && UIUtils.isDark(getDefaultWidgetBackground().getRGB());
    }

    public static Color getDefaultWidgetBackground() {
        org.eclipse.ui.themes.ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
        Color color = theme.getColorRegistry().get(IWorkbenchThemeConstants.INACTIVE_TAB_BG_START);
        if (color == null) {
            color = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        }
        return color;
    }

    public static Color getDefaultTextBackground() {
        return getDefaultTextColor("AbstractTextEditor.Color.Background", SWT.COLOR_LIST_BACKGROUND);
    }

    public static Color getDefaultTextForeground() {
        return getDefaultTextColor("AbstractTextEditor.Color.Foreground", SWT.COLOR_LIST_FOREGROUND);
    }

    public static Color getDefaultTextSelectionBackground() {
        return getDefaultTextColor("AbstractTextEditor.Color.SelectionBackground", SWT.COLOR_LIST_SELECTION);
    }

    public static Color getDefaultTextSelectionForeground() {
        return getDefaultTextColor("AbstractTextEditor.Color.SelectionForeground", SWT.COLOR_LIST_SELECTION_TEXT);
    }

    public static Color getDefaultTextColor(String id, int defSWT) {
        IPreferenceStore preferenceStore = getEditorsPreferenceStore();
        String fgRGB = preferenceStore == null ? null : preferenceStore.getString(id);
        return CommonUtils.isEmpty(fgRGB) ? Display.getDefault().getSystemColor(defSWT) : UIUtils.getSharedColor(fgRGB);
    }

    public static Color getErrorTextForeground() {
        return getDefaultTextColor("AbstractTextEditor.Error.Color.Foreground", SWT.COLOR_RED);
    }


    /**
     * Calculate the Contrast color based on Luma(brightness)
     * https://en.wikipedia.org/wiki/Luma_(video)
     *
     * Do not dispose returned color.
     */
    public static Color getContrastColor(Color color) {
        if (color == null) {
            return COLOR_BLACK;
        }
        double luminance = 1 - (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255;
        if (luminance > 0.5) {
            return isDarkTheme() ? COLOR_WHITE_DARK : COLOR_WHITE;
        }
        return COLOR_BLACK;
    }

    public static Color getInvertedColor(Color color) {
        return new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());
    }

    @NotNull
    public static Color lighten(@NotNull Color color, float amount) {
        // https://github.com/JFormDesigner/FlatLaf/blob/34b19f00e4488292f5dd7869205d41982bed317a/flatlaf-core/src/main/java/com/formdev/flatlaf/util/ColorFunctions.java#L38
        var hsl = toHSL(color);
        var l = hsl[2] + amount;
        return toRGB(hsl[0], hsl[1], l, color.getAlpha() / 255f);
    }

    @NotNull
    public static Color darken(@NotNull Color color, float amount) {
        // https://github.com/JFormDesigner/FlatLaf/blob/34b19f00e4488292f5dd7869205d41982bed317a/flatlaf-core/src/main/java/com/formdev/flatlaf/util/ColorFunctions.java#L52
        var hsl = toHSL(color);
        var l = hsl[2] - amount;
        return toRGB(hsl[0], hsl[1], l, color.getAlpha() / 255f);
    }

    /**
     * Convert an RGB Color to HSL values.
     * <br>
     * Hue is specified as degrees in the range 0 - 360.
     * Saturation and Luminance are specified as percentages in the range 0 - 1.
     *
     * @param color the RGB color
     * @return an array containing HSL values: [Hue, Saturation, Luminance]
     */
    @NotNull
    public static float[] toHSL(@NotNull Color color) {
        // https://github.com/JFormDesigner/FlatLaf/blob/34b19f00e4488292f5dd7869205d41982bed317a/flatlaf-core/src/main/java/com/formdev/flatlaf/util/HSLColor.java#L260
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float min = Math.min(r, Math.min(g, b));
        float max = Math.max(r, Math.max(g, b));

        float h = 0;
        if (max == min) {
            h = 0;
        } else if (max == r) {
            h = ((60 * (g - b) / (max - min)) + 360) % 360;
        } else if (max == g) {
            h = (60 * (b - r) / (max - min)) + 120;
        } else if (max == b) {
            h = (60 * (r - g) / (max - min)) + 240;
        }

        float l = (max + min) / 2;

        float s;
        if (max == min) {
            s = 0;
        } else if (l <= .5f) {
            s = (max - min) / (max + min);
        } else {
            s = (max - min) / (2 - max - min);
        }

        return new float[] {h, s, l};
    }

    /**
     * Convert HSL values to an RGB Color.
     *
     * @param h hue is specified as degrees in the range 0 - 360.
     * @param s saturation is specified as a percentage in the range 1 - 100.
     * @param l luminance is specified as a percentage in the range 1 - 100.
     * @return the RGB color
     */
    @NotNull
    public static Color toRGB(float h, float s, float l, float alpha) {
        // https://github.com/JFormDesigner/FlatLaf/blob/34b19f00e4488292f5dd7869205d41982bed317a/flatlaf-core/src/main/java/com/formdev/flatlaf/util/HSLColor.java#L362
        h = h % 360f / 360f;
        s = Math.clamp(s, 0f, 1f);
        l = Math.clamp(l, 0f, 1f);
        alpha = Math.clamp(alpha, 0f, 1f);

        float q;
        if (l < 0.5) {
            q = l * (1 + s);
        } else {
            q = (l + s) - (s * l);
        }

        float p = 2 * l - q;
        float r = Math.max(0, hueToRGB(p, q, h + (1f / 3f)));
        float g = Math.max(0, hueToRGB(p, q, h));
        float b = Math.max(0, hueToRGB(p, q, h - (1f / 3f)));

        r = Math.min(r, 1f);
        g = Math.min(g, 1f);
        b = Math.min(b, 1f);

        return new Color(
            (int) (r * 255),
            (int) (g * 255),
            (int) (b * 255),
            (int) (alpha * 255)
        );
    }

    private static float hueToRGB(float p, float q, float h) {
        // https://github.com/JFormDesigner/FlatLaf/blob/34b19f00e4488292f5dd7869205d41982bed317a/flatlaf-core/src/main/java/com/formdev/flatlaf/util/HSLColor.java#L409
        if (h < 0) {
            h += 1;
        }
        if (h > 1) {
            h -= 1;
        }
        if (6 * h < 1) {
            return p + ((q - p) * 6 * h);
        }
        if (2 * h < 1) {
            return q;
        }
        if (3 * h < 2) {
            return p + (q - p) * 6 * (2f / 3f - h);
        }
        return p;
    }

    /**
     * Fixes toolbars foreground colors on macOS to ensure proper text visibility
     */
    public static void fixToolBarForeground(@NotNull Collection<ToolBarManager> toolbarManagers) {
        if (!RuntimeUtils.isMacOS()) {
            return;
        }
        for (ToolBarManager toolbarManager : toolbarManagers) {
            ToolBar toolbar = toolbarManager.getControl();
            if (toolbar != null && !toolbar.isDisposed()) {
                fixToolBarForeground(toolbar);
            }
        }
    }

    public static void fixToolBarForeground(@NotNull ToolBar toolBar) {
        if (!toolBar.isDisposed()) {
            Color textColor = getDefaultTextForeground();
            toolBar.setForeground(textColor);
            for (ToolItem item : toolBar.getItems()) {
                item.setForeground(textColor);
            }
        }
    }

}
