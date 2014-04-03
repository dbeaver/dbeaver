/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;

/**
 * GridDataProvider
 */
public interface ISpreadsheetController {

    boolean hasData();

    boolean isReadOnly();

    boolean isInsertable();

    @Nullable
    Control showCellEditor(boolean inline);

    void resetCellValue(@NotNull GridCell cell, boolean delete);

    void fillContextMenu(@NotNull GridCell cell, @NotNull IMenuManager manager);

    void changeSorting(@NotNull Object columnElement, int state);

    void navigateLink(@NotNull GridCell cell, int state);

    @NotNull
    IPreferenceStore getPreferenceStore();
}
