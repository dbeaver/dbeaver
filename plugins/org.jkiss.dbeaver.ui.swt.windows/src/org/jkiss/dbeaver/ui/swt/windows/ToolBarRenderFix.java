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
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.LongKeyMap;

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

    //
    // We are using only one Callback instance shared between all the toolbars to handle, and we dispatch the logic with HashMap of them
    // because for whatever reason SWT uses the approach with hard-coded MAX_CALLBACKS limit
    // and we don't want to implement our own solution to intercept native functions per UI control.
    //     See also
    // https://github.com/eclipse-platform/eclipse.platform.swt/blob/master/bundles/org.eclipse.swt/Eclipse%20SWT/common/org/eclipse/swt/internal/Callback.java
    // https://github.com/eclipse-platform/eclipse.platform.swt/blob/master/bundles/org.eclipse.swt/Eclipse%20SWT/common/library/callback.h
    // https://github.com/eclipse-platform/eclipse.platform.swt/blob/master/bundles/org.eclipse.swt/Eclipse%20SWT/common/library/callback.c
    //

    private DisplayEventFilter eventFilter;

    private Callback windowCallback;
    private final LongKeyMap<ToolbarSubclassHandler> handlersByHwnd = new LongKeyMap<>();

    private volatile boolean isEnabled = false;
    private volatile boolean isDeactivating = false;

    public ToolBarRenderFix() {
    }

    @Override
    public void activateService() {
        if (UIStyles.isDarkTheme() && System.getProperty("os.name").contains("Windows 11") && !this.isEnabled) {
            if (this.windowCallback == null) {
                this.windowCallback = new Callback(this, "customWindowProc", 4); //$NON-NLS-1$
            }
            this.isEnabled = true;
            this.isDeactivating = false;
            this.eventFilter = new DisplayEventFilter(UIUtils.getDisplay(), new int[]{ SWT.Paint, SWT.Resize }, event -> {
                if (event.widget instanceof ToolBar t && t.getData(DBEAVER_TOOLBAR_SUBCLASS_HANDLER_PROP_NAME) == null) {
                    t.setData(DBEAVER_TOOLBAR_SUBCLASS_HANDLER_PROP_NAME, new ToolbarSubclassHandler(t));
                }
            });
        }
    }

    @Override
    public void deactivateService() {
        // we should prevent Callback from dispose while there are any toolbars that could reference it
        if (this.isEnabled) {
            this.isDeactivating = true;
            this.isEnabled = false;
            if (this.eventFilter != null ) {
                this.eventFilter.dispose();
                this.eventFilter = null;
            }
        }
    }

    private void registerHandler(ToolbarSubclassHandler handler) {
        // SetWindowSubclass(..) should have been used here to have formally clear cleanup procedure and per-instance dwRefData value,
        // but SWT uses SetWindowLongPtr(..) and so we are

        this.handlersByHwnd.put(handler.toolBar.handle, handler);
        OS.AllowDarkModeForWindow(handler.toolBar.handle, true);
        OS.SetWindowLongPtr(handler.toolBar.handle, OS.GWLP_WNDPROC, handler.myProcPtr);
    }

    private void unregisterHandler(ToolbarSubclassHandler handler) {
        OS.SetWindowLongPtr(handler.toolBar.handle, OS.GWLP_WNDPROC, handler.prevProcPtr);
        this.handlersByHwnd.remove(handler.toolBar.handle);

        if (this.handlersByHwnd.isEmpty() && this.isDeactivating) {
            this.isDeactivating = false;
            this.windowCallback.dispose();
            this.windowCallback = null;
        }
    }

    protected long customWindowProc(long hwnd, long msg, long wParam, long lParam) {
        ToolbarSubclassHandler handler = this.handlersByHwnd.get(hwnd);
        if (handler != null) {
            return handler.customWindowProc(hwnd, msg, wParam, lParam);
        } else {
            return OS.DefWindowProc(hwnd, (int) msg, wParam, lParam);
        }
    }

    class DisplayEventFilter {

        private final Display display;
        private final int[] eventsToHandle;

        private final Listener listener;

        public DisplayEventFilter(Display display, int[] eventsToHandle, Listener listener) {
            this.display = display;
            this.eventsToHandle = eventsToHandle;
            this.listener = listener;

            for (int eventId : eventsToHandle) {
                display.addFilter(eventId, this.listener);
            }
        }

        public void dispose() {
            for (int eventId : eventsToHandle) {
                if (!display.isDisposed()) {
                    display.removeFilter(eventId, this.listener);
                }
            }
        }
    }

    private class ToolbarSubclassHandler {
        public final ToolBar toolBar;
        public final Font font;
        public final long prevProcPtr;
        public final long myProcPtr;

        public ToolbarSubclassHandler(@NotNull ToolBar toolBar) {
            this.toolBar = toolBar;
            this.font = toolBar.getFont();
            this.prevProcPtr = OS.GetWindowLongPtr(toolBar.handle, OS.GWLP_WNDPROC);
            this.myProcPtr = windowCallback.getAddress();

            this.toolBar.addDisposeListener(e -> ToolBarRenderFix.this.unregisterHandler(this));
            ToolBarRenderFix.this.registerHandler(this);
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
                        rect.left = bb.x - 1;
                        rect.top = bb.y - 1;
                        rect.right = bb.x + bb.width;
                        rect.bottom = bb.y + bb.height;
                        rects.add(rect);
                    }
                }

                if (rects.size() > 0) {
                    // switch theming off and invalidate for default renderer only items we're unhappy with how the theme renders them
                    OS.SetWindowTheme(this.toolBar.handle, null, null);
                    this.toolBar.setFont(font);
                    OS.ValidateRect(this.toolBar.handle, null);
                    for (var rect : rects) {
                        OS.InvalidateRect(this.toolBar.handle, rect, true);
                    }
                    this.callPrevWindowProc(hwnd, (int) msg, wParam, lParam);
                    this.ensureOverride();

                    // switch theming on and render themed toolbar excluding items we've just rendered with the default renderer
                    OS.SetWindowTheme(this.toolBar.handle, EXPLORER, null);
                    this.toolBar.setFont(font);
                    for (var rect : rects) {
                        OS.ValidateRect(this.toolBar.handle, rect);
                    }

                    // leaving the theming on because otherwise visual state is compromised when the background color changed dynamically
                    // (like when activating a fesh instance of colored connection's object editor)
                }
            }

            var rc = this.callPrevWindowProc(hwnd, (int) msg, wParam, lParam);
            this.ensureOverride();
            return rc;
        }
    }
}

