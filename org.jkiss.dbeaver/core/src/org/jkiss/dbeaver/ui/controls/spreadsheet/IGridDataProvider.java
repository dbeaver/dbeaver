/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.eclipse.swt.widgets.Composite;

/**
 * GridDataProvider
 */
public interface IGridDataProvider {

    boolean isEditable();

    boolean isCellEditable(int col, int row);

    boolean isCellModified(int col, int row);

    boolean isInsertable();

    void fillRowData(IGridRowData row);

    boolean showCellEditor(
        IGridRowData row,
        boolean inline,
        Composite inlinePlaceholder);
    
}
