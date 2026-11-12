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

import org.eclipse.swt.SWT;
import org.eclipse.swt.internal.cocoa.NSProgressIndicator;
import org.eclipse.swt.internal.cocoa.OS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.HashSet;
import java.util.Set;

public final class DBeaverProgressBarHelper {
    // cocoa does not implement SWT.PAUSED for progress bars
    private static final long STOP_ANIMATION = OS.sel_registerName("stopAnimation:");

    private final Display display;
    private final Set<ProgressBar> pausedBars = new HashSet<>();
    private boolean updatePending;

    private DBeaverProgressBarHelper(@NotNull Display display) {
        this.display = display;
        Listener listener = event -> {
            if ((event.widget instanceof Shell || event.widget instanceof ProgressBar) && !updatePending) {
                updatePending = true;
                display.asyncExec(() -> {
                    updatePending = false;
                    if (!display.isDisposed()) {
                        updateAnimations();
                    }
                });
            }
        };
        display.addFilter(SWT.Show, listener);
        display.addFilter(SWT.Hide, listener);
        display.addFilter(SWT.Dispose, listener);
        display.disposeExec(pausedBars::clear);
        updateAnimations();
    }

    public static void install(@NotNull Display display) {
        new DBeaverProgressBarHelper(display);
    }

    private void updateAnimations() {
        pausedBars.removeIf(Widget::isDisposed);
        for (Shell shell : display.getShells()) {
            updateAnimations(shell, UIUtils.isShellBlocked(shell));
        }
    }

    private void updateAnimations(@NotNull Control control, boolean blocked) {
        if (control instanceof ProgressBar bar && (bar.getStyle() & SWT.INDETERMINATE) != 0) {
            if (!bar.isVisible() || blocked) {
                pausedBars.add(bar);
                // cocoa can restart animation when the view moves to a window
                OS.objc_msgSend(bar.view.id, STOP_ANIMATION, 0L);
            } else if (pausedBars.remove(bar)) {
                ((NSProgressIndicator) bar.view).startAnimation(null);
            }
        } else if (control instanceof Composite composite) {
            for (Control child : composite.getChildren()) {
                updateAnimations(child, blocked);
            }
        }
    }
}
