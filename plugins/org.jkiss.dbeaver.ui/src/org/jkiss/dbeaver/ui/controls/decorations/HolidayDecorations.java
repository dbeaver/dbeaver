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
package org.jkiss.dbeaver.ui.controls.decorations;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.services.IDisposable;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

import java.awt.*;
import java.time.LocalDate;

/**
 * This class is responsible for drawing various random decorations over a control.
 * <p>
 * Currently, it only provides winter decorations, such as snowflakes.
 *
 * @see #install(Control)
 * @see #isEnabled()
 */
public class HolidayDecorations implements IDisposable {
    public static final String PREF_UI_SHOW_HOLIDAY_DECORATIONS = "ui.show.holiday.decorations"; //$NON-NLS-1$

    private static final boolean ENABLED = isEnabled0();

    private final Painter painter;
    private double frameTime;

    private HolidayDecorations(@NotNull Control control) {
        this.painter = new SnowflakePainter(control.getDisplay());

        final Listener listener = event -> {
            switch (event.type) {
                case SWT.Resize -> reset(((Control) event.widget).getSize());
                case SWT.Paint -> paint(event.gc);
                case SWT.Dispose -> dispose();
            }
        };

        control.addListener(SWT.Resize, listener);
        control.addListener(SWT.Paint, listener);
        control.addListener(SWT.Dispose, listener);
    }

    public static boolean install(@NotNull Control control) {
        if (!isEnabled()) {
            return false;
        }

        // Is there any better way to obtain display's refresh rate?
        final GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice device = environment.getDefaultScreenDevice();
        final int refreshRate = 1000 / Math.max(device.getDisplayMode().getRefreshRate(), 60);

        final HolidayDecorations decorations = new HolidayDecorations(control);
        final Display display = Display.getCurrent();

        final Runnable timer = new Runnable() {
            @Override
            public void run() {
                if (control.isDisposed()) {
                    return;
                }

                decorations.update(control.getSize());

                if (control.isVisible()) {
                    display.asyncExec(() -> {
                        if (!control.isDisposed()) {
                            control.redraw();
                        }
                    });

                    display.timerExec(refreshRate, this);
                } else {
                    // Throttle a bit
                    display.timerExec(refreshRate * 10, this);
                }
            }
        };

        display.timerExec(refreshRate, timer);
        UIUtils.enableDoubleBuffering(control);

        return true;
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    private static boolean isEnabled0() {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(PREF_UI_SHOW_HOLIDAY_DECORATIONS)) {
            return false;
        }
        // Dec 24 <= Cur <= Jan 7
        LocalDate current = LocalDate.now();
        return switch (current.getMonth()) {
            case DECEMBER -> current.getDayOfMonth() >= 24;
            case JANUARY -> current.getDayOfMonth() <= 7;
            default -> false;
        };
    }

    @Override
    public void dispose() {
        painter.dispose();
    }

    private void paint(@NotNull GC gc) {
        painter.paint(gc);
    }

    private void reset(@NotNull Point size) {
        if (size.x <= 0 || size.y <= 0) {
            return;
        }

        frameTime = System.currentTimeMillis() / 1000.0;
        painter.reset(size.x, size.y);
    }

    private void update(@NotNull Point size) {
        if (size.x <= 0 || size.y <= 0) {
            return;
        }

        double currentTime = System.currentTimeMillis() / 1000.0;
        double deltaTime = currentTime - frameTime;

        frameTime = currentTime;
        painter.update(size.x, size.y, deltaTime);
    }
}
