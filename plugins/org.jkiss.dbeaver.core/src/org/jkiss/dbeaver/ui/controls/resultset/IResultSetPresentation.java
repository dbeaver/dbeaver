/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;

/**
 * Result set renderer.
 * Visualizes result set viewer/editor.
 *
 * May additionally implement IResultSetEditor, ISelectionProvider, IStatefulControl
 */
public interface IResultSetPresentation {

    String PRES_TOOLS_BEGIN = "rsv_pres_begin";
    String PRES_TOOLS_END = "rsv_pres_end";

    enum RowPosition {
        FIRST,
        PREVIOUS,
        NEXT,
        LAST
    }

    void createPresentation(@NotNull IResultSetController controller, @NotNull Composite parent);

    Control getControl();

    void refreshData(boolean refreshMetadata, boolean append);

    /**
     * Called after results refresh
     * @param refreshData data was refreshed
     */
    void formatData(boolean refreshData);

    void clearMetaData();

    void updateValueView();

    /**
     * Called by controller to fill edit toolbar
     * @param toolBar    toolbar
     */
    void fillToolbar(@NotNull IToolBarManager toolBar);

    /**
     * Called by controller to fill context menu.
     * Note: context menu invocation must be initiated by presentation, then it should call controller's
     * {@link org.jkiss.dbeaver.ui.controls.resultset.IResultSetController#fillContextMenu} which then will
     * call this function.
     * Cool, huh?
     * @param menu    manu
     */
    void fillMenu(@NotNull IMenuManager menu);

    void changeMode(boolean recordMode);

    void scrollToRow(@NotNull RowPosition position);

    @Nullable
    DBDAttributeBinding getCurrentAttribute();

    @Nullable
    String copySelectionToString(
        boolean copyHeader,
        boolean copyRowNumbers,
        boolean cut,
        String delimiter,
        DBDDisplayFormat format);

}
