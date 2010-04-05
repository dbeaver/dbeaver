package org.jkiss.dbeaver.ui.controls.grid;

import org.eclipse.swt.widgets.Widget;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableItem;

/**
 * GridDataProvider
 */
public interface IGridDataProvider {

    boolean isEditable();

    boolean isInsertable();

    void fillRowData(IGridRow row);

    boolean showCellEditor(
        IGridRow row,
        boolean inline,
        Composite inlinePlaceholder);
    
    String getRowTitle(int rowNum);
}
