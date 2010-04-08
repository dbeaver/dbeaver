package org.jkiss.dbeaver.ui.controls.grid;

import org.eclipse.swt.widgets.Composite;

/**
 * GridDataProvider
 */
public interface IGridDataProvider {

    boolean isEditable();

    boolean isCellEditable(int column, int row);

    boolean isInsertable();

    void fillRowData(IGridRowData row);

    boolean showCellEditor(
        IGridRowData row,
        boolean inline,
        Composite inlinePlaceholder);
    
    void fillRowInfo(int rowNum, IGridRowInfo rowInfo);

}
