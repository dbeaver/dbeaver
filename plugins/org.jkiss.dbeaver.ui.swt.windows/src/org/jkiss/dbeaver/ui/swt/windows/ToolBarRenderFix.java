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
package org.jkiss.dbeaver.ui.swt.windows;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.internal.Win32DPIUtils;
import org.eclipse.swt.internal.win32.OS;
import org.eclipse.swt.internal.win32.RECT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Fixes white (or light blue) background for toolbar items under the mouse for Windows 11 and Dark theme
 * https://github.com/dbeaver/pro/issues/9018
 *
 * We intercept the window function to handle the WM_PAINT message of the toolbar UI control,
 * switching theming off and rendering items we're unhappy with with the default renderer instead of a bad-looking one.
 */
public class ToolBarRenderFix implements IPluginService {

    private static final String DBEAVER_TOOLBAR_SUBCLASS_HANDLER_PROP_NAME = "DBEAVER_TOOLBAR_SUBCLASS_HANDLER";

    private static final char[] EXPLORER = "EXPLORER".toCharArray();

    public ToolBarRenderFix() {
    }

    @Override
    public void activateService() {
        if (UIStyles.isDarkTheme() && System.getProperty("os.name").contains("Windows 11")) {
            addDarkThemeToolbarsFix();
        }
    }

    @Override
    public void deactivateService() {

    }

    private void addDarkThemeToolbarsFix() {
        int[] eventsToHandle = new int[]{ SWT.Paint, SWT.Resize };

        Listener listener = event -> {
            if (event.widget instanceof ToolBar t && t.getData(DBEAVER_TOOLBAR_SUBCLASS_HANDLER_PROP_NAME) == null) {
                t.setData(DBEAVER_TOOLBAR_SUBCLASS_HANDLER_PROP_NAME, new ToolbarSubclassHandler(t));
            }
        };

        Display display = UIUtils.getDisplay();
        for (int eventId : eventsToHandle) {
            display.addFilter(eventId, listener);
        }
    }

    private class ToolbarSubclassHandler {
        private final ToolBar toolBar;
        private final Font font;
        private final long prevProcPtr;
        private final Callback windowCallback;
        private final long myProcPtr;

        public ToolbarSubclassHandler(@NotNull ToolBar toolBar) {
            this.toolBar = toolBar;
            this.font = toolBar.getFont();
            this.prevProcPtr = OS.GetWindowLongPtr(toolBar.handle, OS.GWLP_WNDPROC);
            this.windowCallback = new Callback(this, "customWindowProc", 4); //$NON-NLS-1$
            this.myProcPtr = windowCallback.getAddress();

            OS.AllowDarkModeForWindow(this.toolBar.handle, true);
            OS.SetWindowLongPtr(this.toolBar.handle, OS.GWLP_WNDPROC, this.myProcPtr);
        }

        private void ensureOverride() {
            long procPtr = OS.GetWindowLongPtr(this.toolBar.handle, OS.GWLP_WNDPROC);
            if (procPtr != this.myProcPtr) {
                OS.SetWindowLongPtr(this.toolBar.handle, OS.GWLP_WNDPROC, this.myProcPtr);
            }
        }

        private long callPrevWindowProc(long hwnd, long msg, long wParam, long lParam) {
            return OS.CallWindowProc(this.prevProcPtr, hwnd, (int) msg, wParam, lParam);
        }

        public long customWindowProc(long hwnd, long msg, long wParam, long lParam) {
            this.ensureOverride();
            if (msg == OS.WM_PAINT) {
                var items = this.toolBar.getItems();
                final int inclusiveMask = SWT.PUSH | SWT.CHECK | SWT.RADIO;
                final int exclusiveMask = SWT.SEPARATOR | SWT.DROP_DOWN;

                List<RECT> rects = new ArrayList<>(items.length);
                for (var item : items) { // obtain rects of toolItems to fix
                    if (item.isEnabled() &&
                        (item.getStyle() & inclusiveMask) > 0 &&
                        (item.getStyle() & exclusiveMask) == 0 &&
                        item.getControl() == null
                    ) {
                        Rectangle bb = Win32DPIUtils.pointToPixel(item.getBounds(),  DPIUtil.getZoomForAutoscaleProperty(item.nativeZoom));
                        var rect = new RECT();
                        rect.left = bb.x;
                        rect.top = bb.y;
                        rect.right = bb.x + bb.width;
                        rect.bottom = bb.y + bb.height;
                        rects.add(rect);
                    }
                }

                if (rects.size() > 0) {
                    // switch theming on and render themed toolbar excluding items to fix
                    OS.SetWindowTheme(this.toolBar.handle, EXPLORER, null);
                    this.toolBar.setFont(font);

                    for (var rect : rects) {
                        OS.ValidateRect(this.toolBar.handle, rect);
                    }

                    this.callPrevWindowProc(hwnd, (int) msg, wParam, lParam);
                    this.ensureOverride();

                    // switch theming off and invalidate for default renderer only items we're unhappy with how the theme renders them
                    OS.SetWindowTheme(this.toolBar.handle, null, null);
                    this.toolBar.setFont(font);
                    OS.ValidateRect(this.toolBar.handle, null);
                    for (var rect : rects) {
                        OS.InvalidateRect(this.toolBar.handle, rect, true);
                    }
                }
            }

            var rc = this.callPrevWindowProc(hwnd, (int) msg, wParam, lParam);
            this.ensureOverride();
            return rc;
        }
    }
}

