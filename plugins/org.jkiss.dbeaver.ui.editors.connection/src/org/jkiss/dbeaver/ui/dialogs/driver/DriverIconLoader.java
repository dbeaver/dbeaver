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
package org.jkiss.dbeaver.ui.dialogs.driver;

import org.eclipse.jface.viewers.StructuredViewer;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverWithLazyIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.ref.WeakReference;

final class DriverIconLoader {
    private static final String CALLBACK_KEY = DriverIconLoader.class.getName() + ".callback";
    private static final String REDRAW_PENDING_KEY = DriverIconLoader.class.getName() + ".redrawPending";

    private DriverIconLoader() {
    }

    static void load(@NotNull DBPDriver driver, @NotNull StructuredViewer viewer) {
        if (driver instanceof DBPDriverWithLazyIcon lazyIcon) {
            var control = viewer.getControl();
            Runnable callback = (Runnable) control.getData(CALLBACK_KEY);
            if (callback == null) {
                WeakReference<StructuredViewer> viewerReference = new WeakReference<>(viewer);
                callback = () -> UIUtils.asyncExec(() -> {
                    StructuredViewer activeViewer = viewerReference.get();
                    if (activeViewer == null || activeViewer.getControl().isDisposed()) {
                        return;
                    }
                    var activeControl = activeViewer.getControl();
                    if (Boolean.TRUE.equals(activeControl.getData(REDRAW_PENDING_KEY))) {
                        return;
                    }
                    activeControl.setData(REDRAW_PENDING_KEY, true);
                    activeControl.getDisplay().timerExec(200, () -> {
                        if (!activeControl.isDisposed()) {
                            activeControl.setData(REDRAW_PENDING_KEY, false);
                            activeControl.redraw();
                        }
                    });
                });
                control.setData(CALLBACK_KEY, callback);
            }
            lazyIcon.loadIcon(callback);
        }
    }
}
