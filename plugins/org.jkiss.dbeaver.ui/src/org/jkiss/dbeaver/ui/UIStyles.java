/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.IWorkbenchThemeConstants;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * UI Utils
 */
public class UIStyles {

    private static final Log log = Log.getLog(UIStyles.class);

    private static final String THEME_HIGH_CONTRAST_ID = "org.eclipse.e4.ui.css.theme.high-contrast";

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
}
