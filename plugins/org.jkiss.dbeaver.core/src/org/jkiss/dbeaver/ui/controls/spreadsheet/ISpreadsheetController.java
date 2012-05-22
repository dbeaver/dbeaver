/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridColumn;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;

/**
 * GridDataProvider
 */
public interface ISpreadsheetController {

    boolean isEditable();

    boolean isCellEditable(GridPos pos);

    boolean isInsertable();

    boolean showCellEditor(
        GridPos cell,
        boolean inline,
        Composite inlinePlaceholder);

    void resetCellValue(GridPos cell, boolean delete);

    void fillContextMenu(
        GridPos cell,
        IMenuManager manager);

    void changeSorting(
        GridColumn column,
        int state);
}
