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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

final class MacOsNativeScale {
    private static final String DISPLAY_KEY = MacOsNativeScale.class.getName();
    private static final String READY_PROPERTY = "dbeaver.ui.nativeScale.ready";
    private static final String ERROR_PROPERTY = "dbeaver.ui.nativeScale.error";
    private static final String FACTOR_PROPERTY = "dbeaver.ui.nativeScale.factor";
    private static final int MAX_DIALOG_PERCENT = 90;
    private static final Log log = Log.getLog(MacOsNativeScale.class);

    private final Field viewField;
    private final Method frameMethod;
    private final Method boundsMethod;
    private final Method setBoundsSizeMethod;
    private final Constructor<?> sizeConstructor;
    private final Field sizeWidth;
    private final Field sizeHeight;
    private final Field rectWidth;
    private final Field rectHeight;
    private final Map<Shell, ViewportSize> initialized = new IdentityHashMap<>();
    private int percentage = MacOsUiScale.DEFAULT_SCALE;
    private boolean updating;

    private MacOsNativeScale(@NotNull Display display) throws ReflectiveOperationException {
        ClassLoader loader = Display.class.getClassLoader();
        Class<?> viewClass = Class.forName("org.eclipse.swt.internal.cocoa.NSView", false, loader);
        Class<?> sizeClass = Class.forName("org.eclipse.swt.internal.cocoa.NSSize", false, loader);
        Class<?> rectClass = Class.forName("org.eclipse.swt.internal.cocoa.NSRect", false, loader);
        viewField = Control.class.getField("view");
        frameMethod = viewClass.getMethod("frame");
        boundsMethod = viewClass.getMethod("bounds");
        setBoundsSizeMethod = viewClass.getMethod("setBoundsSize", sizeClass);
        sizeConstructor = sizeClass.getConstructor();
        sizeWidth = sizeClass.getField("width");
        sizeHeight = sizeClass.getField("height");
        rectWidth = rectClass.getField("width");
        rectHeight = rectClass.getField("height");
        display.addFilter(SWT.Resize, event -> {
            if (!updating && event.widget instanceof Shell shell && initialized.containsKey(shell)) {
                updateShell(shell, 1, true);
            }
        });
        display.addFilter(SWT.Show, event -> {
            if (event.widget instanceof Shell shell && !updating) {
                updateShell(shell, !initialized.containsKey(shell) && shell.getParent() != null
                    ? percentage / (double) MacOsUiScale.DEFAULT_SCALE : 1);
            }
        });
        display.disposeExec(initialized::clear);
    }

    static boolean isAvailable() {
        return Boolean.getBoolean(READY_PROPERTY) && System.getProperty(ERROR_PROPERTY) == null;
    }

    static void apply(@NotNull Display display, int percentage) {
        try {
            MacOsNativeScale manager = (MacOsNativeScale) display.getData(DISPLAY_KEY);
            if (manager == null) {
                manager = new MacOsNativeScale(display);
                display.setData(DISPLAY_KEY, manager);
            }
            int previousPercentage = manager.percentage;
            manager.percentage = percentage;
            System.setProperty(FACTOR_PROPERTY, Double.toString(percentage / (double) MacOsUiScale.DEFAULT_SCALE));
            for (Shell shell : display.getShells()) {
                if (!shell.isDisposed() && shell.isVisible()) {
                    manager.updateShell(shell, shell.getParent() == null ? 1 : percentage / (double) previousPercentage);
                }
            }
            log.debug("Native macOS UI scale: " + percentage + "%");
        } catch (ReflectiveOperationException exception) {
            log.error("Cannot initialize native macOS UI scale", exception);
        }
    }

    private void updateShell(@NotNull Shell shell, double resizeFactor) {
        updateShell(shell, resizeFactor, false);
    }

    private void updateShell(@NotNull Shell shell, double resizeFactor, boolean rememberSize) {
        updating = true;
        try {
            Object view = viewField.get(shell);
            if (view == null) {
                return;
            }
            Object frame = frameMethod.invoke(view);
            double width = rectWidth.getDouble(frame);
            double height = rectHeight.getDouble(frame);
            double factor = percentage / (double) MacOsUiScale.DEFAULT_SCALE;
            ViewportSize viewport = initialized.get(shell);
            if (viewport == null) {
                viewport = new ViewportSize();
                viewport.width = width * resizeFactor / factor;
                viewport.height = height * resizeFactor / factor;
                initialized.put(shell, viewport);
                shell.addDisposeListener(event -> initialized.remove(shell));
            } else if (rememberSize) {
                viewport.width = width / factor;
                viewport.height = height / factor;
            }
            if (resizeFactor != 1) {
                Rectangle windowBounds = shell.getBounds();
                Point windowSize = shell.getSize();
                Rectangle monitor = shell.getMonitor().getClientArea();
                // derive every zoom from logical dimensions so rounding never accumulates across switches
                int targetWidth = (int) Math.round(viewport.width * factor) + windowSize.x - (int) width;
                int targetHeight = (int) Math.round(viewport.height * factor) + windowSize.y - (int) height;
                shell.setSize(
                    Math.min(targetWidth, monitor.width * MAX_DIALOG_PERCENT / MacOsUiScale.DEFAULT_SCALE),
                    Math.min(targetHeight, monitor.height * MAX_DIALOG_PERCENT / MacOsUiScale.DEFAULT_SCALE)
                );
                Point adjusted = shell.getSize();
                int left = windowBounds.x + (windowSize.x - adjusted.x) / 2;
                int top = windowBounds.y + (windowSize.y - adjusted.y) / 2;
                shell.setLocation(
                    Math.max(monitor.x, Math.min(left, monitor.x + monitor.width - adjusted.x)),
                    Math.max(monitor.y, Math.min(top, monitor.y + monitor.height - adjusted.y))
                );
                frame = frameMethod.invoke(view);
                width = rectWidth.getDouble(frame);
                height = rectHeight.getDouble(frame);
            }
            Object size = sizeConstructor.newInstance();
            sizeWidth.setDouble(size, width / factor);
            sizeHeight.setDouble(size, height / factor);
            setBoundsSizeMethod.invoke(view, size);
            shell.layout(true, true);
            shell.redraw();
            Object bounds = boundsMethod.invoke(view);
            log.debug("Native scale shell '" + shell.getText() + "': frame " + width + "x" + height
                + ", logical " + rectWidth.getDouble(bounds) + "x" + rectHeight.getDouble(bounds));
        } catch (ReflectiveOperationException exception) {
            log.error("Cannot scale macOS shell", exception);
        } finally {
            updating = false;
        }
    }

    private static final class ViewportSize {
        private double width;
        private double height;
    }
}
