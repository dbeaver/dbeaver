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
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.internal.win32.MENUITEMINFO;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.internal.win32.RECT;

import java.util.function.BooleanSupplier;

/**
 * SWT theme switch helpers
 */
public final class SWTThemeUtils {

    public static final char[] WINDOW_THEME_COMBO = "CFD\0".toCharArray();
    public static final char[] WINDOW_THEME_DEFAULT = Display.EXPLORER;
    private static final int FLUSH_MENU_THEMES_ORDINAL = 136;
    // Windows TREEITEMSTATES.HOTSELECTED is not exposed by SWT's OS constants.
    private static final int TREIS_HOTSELECTED = 6;

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

    public static void installTreeSelectionFix(Tree tree, BooleanSupplier isDarkTheme) {
        tree.addListener(SWT.EraseItem, new TreeSelectionFix(tree, isDarkTheme)::eraseItem);
    }

    private static final class TreeSelectionFix {
        private final Tree tree;
        private final BooleanSupplier isDarkTheme;
        private TreeItem hotItem;

        private TreeSelectionFix(Tree tree, BooleanSupplier isDarkTheme) {
            this.tree = tree;
            this.isDarkTheme = isDarkTheme;
        }

        private void eraseItem(Event event) {
            if (!isDarkTheme.getAsBoolean() || tree.getColumnCount() < 2) {
                return;
            }
            boolean selected = (event.detail & SWT.SELECTED) != 0;
            if (event.index == tree.getFirstColumnIndex()) {
                Point cursor = tree.toControl(tree.getDisplay().getCursorLocation());
                hotItem = (event.detail & SWT.HOT) != 0 || tree.getItem(cursor) == event.item
                    ? (TreeItem) event.item : null;
            }
            // Suppressing the native first-cell HOT state also clears CDIS_HOT before SWT paints
            // subsequent columns. Remember it from the first cell to paint the entire hovered row.
            boolean hot = event.item == hotItem;
            if (!selected && !hot) {
                return;
            }

            int zoom = tree.getAutoscalingZoom();
            long theme = OS.OpenThemeData(tree.handle, Display.TREEVIEW, zoom);
            if (theme == 0) {
                return;
            }
            try {
                RECT row = new RECT();
                Rectangle bounds = tree.getClientArea();
                row.left = DPIUtil.pointToPixel(bounds.x, zoom);
                row.right = DPIUtil.pointToPixel(bounds.x + bounds.width, zoom);
                int totalWidth = 0;
                for (TreeColumn column : tree.getColumns()) {
                    totalWidth += column.getWidth();
                }
                if (totalWidth > bounds.width) {
                    row.left = 0;
                    row.right = DPIUtil.pointToPixel(totalWidth, zoom);
                }
                row.top = DPIUtil.pointToPixel(event.y, zoom);
                row.bottom = DPIUtil.pointToPixel(event.y + event.height, zoom);

                RECT cell = new RECT();
                cell.left = DPIUtil.pointToPixel(event.x, zoom);
                cell.right = DPIUtil.pointToPixel(event.x + event.width, zoom);
                cell.top = row.top;
                cell.bottom = row.bottom;
                if (totalWidth < bounds.width && event.index == tree.getColumnOrder()[tree.getColumnCount() - 1]) {
                    // SWT normally extends the last selected cell to the right edge of the tree.
                    cell.right = row.right;
                    event.gc.setClipping((Rectangle) null);
                }
                // Use Windows' combined state on hover, but retain the native inactive selection state.
                int state = selected ? (hot ? TREIS_HOTSELECTED : OS.TREIS_SELECTED) : OS.TREIS_HOT;
                if (selected && !hot && !tree.isFocusControl()) {
                    state = OS.TREIS_SELECTEDNOTFOCUS;
                }
                OS.DrawThemeBackground(theme, event.gc.handle, OS.TVP_TREEITEM, state, row, cell);
                if (selected) {
                    event.gc.setForeground(tree.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
                }
                event.detail &= ~(SWT.SELECTED | SWT.HOT | SWT.BACKGROUND);
            } finally {
                OS.CloseThemeData(theme);
            }
        }
    }

    private static char[] getDarkThemeIdByWidgetType(Control control) {
        // Combo is an exception?
        return control instanceof Combo ? WINDOW_THEME_COMBO : WINDOW_THEME_DEFAULT;
    }
}
