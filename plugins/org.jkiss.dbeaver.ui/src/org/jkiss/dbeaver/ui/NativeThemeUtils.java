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
package org.jkiss.dbeaver.ui;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.event.EventHandler;

import java.util.function.BooleanSupplier;

/**
 * This is a hack for SWT Light/Dark theme switch.
 * It relies on Windows native controls theme update.
 * TODO: remove when it will be fixed in Eclipse SWT
 */
public final class NativeThemeUtils {
    private static final Log log = Log.getLog(NativeThemeUtils.class);
    private static boolean themeListenerInstalled;

    private static final Class<?>[] RESKIN_WIDGET_TYPES = new Class[] {
        Button.class,
        Label.class,
        Combo.class,
        Text.class,
        Tree.class,
        Table.class,
    };

    public static void installThemeListener(@NotNull Display display) {
        if (!RuntimeUtils.isWindows() || themeListenerInstalled) {
            return;
        }
        themeListenerInstalled = true;
        display.addListener(SWT.Skin, event -> {
            if (event.widget instanceof Control control && ArrayUtils.contains(RESKIN_WIDGET_TYPES, control.getClass())) {
                updateNativeWidgets(control);
            }
        });
        IEventBroker eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
        if (eventBroker != null) {
            EventHandler themeListener = event -> display.asyncExec(() -> {
                if (!display.isDisposed()) {
                    updateNativeShellsAndMenus(display);
                }
            });
            eventBroker.subscribe(IThemeEngine.Events.THEME_CHANGED, themeListener);
            display.disposeExec(() -> eventBroker.unsubscribe(themeListener));
        }
    }

    private static void updateNativeShellsAndMenus(@NotNull Display display) {
        try {
            Class<?> themeUtils = getNativeUtilsClass();
            themeUtils.getMethod("updateShellsAndMenus", Display.class, boolean.class)
                .invoke(null, display, UIStyles.isDarkTheme());
        } catch (Throwable e) {
            log.debug("Error updating native shell and menu theme", e);
        }
    }

    private static void updateNativeWidgets(@NotNull Control control) {
        try {
            Class<?> themeUtils = getNativeUtilsClass();
            themeUtils.getMethod("updateWidgetTheme", Control.class, boolean.class)
                .invoke(null, control, UIStyles.isDarkTheme());
        } catch (Throwable e) {
            log.debug("Error updating native control theme", e);
        }
    }

    public static void installTreeSelectionFix(@NotNull Tree tree) {
        if (!RuntimeUtils.isWindows()) {
            return;
        }
        try {
            getNativeUtilsClass().getMethod("installTreeSelectionFix", Tree.class, BooleanSupplier.class)
                .invoke(null, tree, (BooleanSupplier) UIStyles::isDarkTheme);
        } catch (Throwable e) {
            log.debug("Error installing native tree selection fix", e);
        }
    }

    private static @NotNull Class<?> getNativeUtilsClass() throws ClassNotFoundException {
        return FrameworkUtil.getBundle(Control.class).loadClass(
            "org.eclipse.swt.widgets.SWTThemeUtils");
    }
}
