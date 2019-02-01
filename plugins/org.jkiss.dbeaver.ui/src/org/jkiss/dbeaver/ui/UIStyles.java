/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;

/**
 * UI Utils
 */
public class UIStyles {

    private static final Log log = Log.getLog(UIStyles.class);

    static IPreferenceStore EDITORS_PREFERENCE_STORE;

    public static synchronized IPreferenceStore getEditorsPreferenceStore() {
        if (EDITORS_PREFERENCE_STORE == null) {
            EDITORS_PREFERENCE_STORE = new ScopedPreferenceStore(InstanceScope.INSTANCE, "org.eclipse.ui.editors");
        }
        return EDITORS_PREFERENCE_STORE;
    }

    public static boolean isDarkTheme() {
        return UIUtils.isDark(getDefaultWidgetBackground().getRGB());
    }

    public static Color getDefaultWidgetBackground() {
        ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
        Color color = theme.getColorRegistry().get("org.eclipse.ui.workbench.INACTIVE_TAB_BG_START");
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

}
