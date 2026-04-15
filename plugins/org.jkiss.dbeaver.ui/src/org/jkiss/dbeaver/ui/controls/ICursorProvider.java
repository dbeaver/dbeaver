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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Interface to provide cursor representation for a given element.
 */
public interface ICursorProvider {
    /**
     * Enables cursor support for the given viewer. The viewer's label provider must implement {@link ICursorProvider}.
     *
     * @param viewer the viewer to enable cursor support for
     */
    static void enableFor(@NotNull ColumnViewer viewer) {
        Control control = viewer.getControl();
        control.addMouseMoveListener(e -> {
            ViewerCell cell = viewer.getCell(new Point(e.x, e.y));
            Cursor cursor = null;
            if (cell != null && viewer.getLabelProvider(cell.getColumnIndex()) instanceof ICursorProvider provider) {
                cursor = provider.getCursor(cell.getElement());
            }
            if (control.getCursor() != cursor) {
                control.setCursor(cursor);
            }
        });
    }

    @Nullable
    Cursor getCursor(@NotNull Object element);
}
