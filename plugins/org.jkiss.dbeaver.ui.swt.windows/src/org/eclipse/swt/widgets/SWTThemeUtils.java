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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.win32.MENUITEMINFO;
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
            Menu menuBar = shell.getMenuBar();
            if (menuBar != null) {
                updateMenuBarTheme(menuBar);
            }
            if (shell.menus != null) {
                updateMenusTheme(shell.menus);
            }
        }
    }

    private static void updateMenuBarTheme(Menu menuBar) {
        Display display = menuBar.getDisplay();
        if (menuBar.foreground != display.menuBarForegroundPixel || menuBar.background != display.menuBarBackgroundPixel) {
            menuBar.initThemeColors();
            menuBar.updateBackground();
            menuBar.updateForeground();
            boolean callback = menuBar.needsMenuCallback();
            MenuItem[] items = menuBar.getItems();
            for (int i = 0; i < items.length; i++) {
                // SWT chooses both the owner-draw type and bitmap callback when it creates a menu item.
                // Menu.updateForeground() changes only the bitmap, leaving Windows to draw the text twice.
                MENUITEMINFO info = new MENUITEMINFO();
                info.cbSize = MENUITEMINFO.sizeof;
                info.fMask = OS.MIIM_FTYPE;
                if (OS.GetMenuItemInfo(menuBar.handle, i, true, info)) {
                    info.fType = callback ? OS.MFT_OWNERDRAW : items[i].widgetStyle();
                    info.fMask = OS.MIIM_FTYPE | OS.MIIM_BITMAP;
                    info.hbmpItem = callback ? OS.HBMMENU_CALLBACK : items[i].hBitmap;
                    OS.SetMenuItemInfo(menuBar.handle, i, true, info);
                    if (!callback && items[i].getImage() != null) {
                        // Restore image handles that were not created while the dark menu used callbacks.
                        var image = items[i].getImage();
                        items[i].setImage(null);
                        items[i].setImage(image);
                    }
                }
            }
        }
        menuBar.update();
    }

    private static void updateMenusTheme(Menu [] menus) {
        for (Menu menu : menus) {
            if (menu == null || menu.isDisposed()) {
                continue;
            }
            for (MenuItem item : menu.getItems()) {
                if (item.imageSelected != null && item.getImage() != null) {
                    // SWT's Win11 checked-item image contains the menu colors from its creation.
                    Image image = item.getImage();
                    item.setImage(null);
                    item.setImage(image);
                }
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
