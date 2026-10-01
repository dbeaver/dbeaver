/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.eclipse.swt.widgets;

import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import org.eclipse.swt.internal.win32.OS;

/**
 * SWT theme switch helpers
 */
public final class SWTThemeUtils {

    public static final char[] WINDOW_THEME_COMBO = "CFD\0".toCharArray();
    public static final char[] WINDOW_THEME_DEFAULT = Display.EXPLORER;
    private static final int FLUSH_MENU_THEMES_ORDINAL = 136;

    public static void updateShellsAndMenus(Display display, boolean dark) {
        // SetPreferredAppMode updates SWT's defaults, but Windows keeps the old menu theme cached.
        if (OS.IsDarkModeAvailable()) {
            try {
                flushMenuTheme();
            } catch (Throwable ignored) {
                // ignore
            }
        }
        for (Shell shell : display.getShells()) {
            shell.setDarkThemePreferred(dark);
            if (shell.getMenuBar() != null) {
                OS.DrawMenuBar(shell.handle);
            }
        }
    }

    private static void flushMenuTheme() {
        var uxtheme = Kernel32.INSTANCE.LoadLibraryEx("uxtheme.dll", null, 0);
        if (uxtheme != null) {
            try {
                Pointer flushMenuThemes = Kernel32.INSTANCE.GetProcAddress(uxtheme, FLUSH_MENU_THEMES_ORDINAL);
                if (flushMenuThemes != null) {
                    Function.getFunction(flushMenuThemes).invokeVoid(new Object[0]);
                }
            } finally {
                Kernel32.INSTANCE.FreeLibrary(uxtheme);
            }
        }
    }

    /**
     * Uses Windows-specific calls and constants to update native widgets look-and-feel
     */
    public static void updateWidgetTheme(Control control, boolean dark) {
        // TODO: do not style custom wigets and empty composites
        OS.AllowDarkModeForWindow(control.handle, dark);
        // For Tree and Table we shouldn't set any theme but EXPLORER.
        // Setting to NULL makes Tree legacy-styled widget.
        boolean isBrokenCtrl = control instanceof Table || control instanceof Tree;
        // For some reason ToolBar looks-n-feel become corrupted after theme set
        if (!(control instanceof ToolBar)) {
            OS.SetWindowTheme(
                control.handle,
                dark || isBrokenCtrl ? getDarkThemeIdByWidgetType(control) : null,
                null
            );
        }
    }

    private static char[] getDarkThemeIdByWidgetType(Control control) {
        // Combo is an exception?
        return control instanceof Combo ? WINDOW_THEME_COMBO : WINDOW_THEME_DEFAULT;
    }
}
