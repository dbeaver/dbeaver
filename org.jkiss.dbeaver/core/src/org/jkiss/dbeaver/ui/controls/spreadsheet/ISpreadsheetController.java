/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.controls.lightgrid.IGridContentProvider;

/**
 * GridDataProvider
 */
public interface ISpreadsheetController {

    boolean isEditable();

    boolean isCellEditable(int col, int row);

    boolean isInsertable();

    boolean showCellEditor(
        int column,
        int row,
        boolean inline,
        Composite inlinePlaceholder);
    
}
