package org.jkiss.dbeaver.ui.controls.grid;

/**
 * GridDataProvider
 */
public interface IGridDataProvider {

    boolean isEditable();

    boolean isInsertable();

    void fillLazyRow(IGridRow row);

    void showRowViewer(IGridRow row, boolean editable);

    String getRowTitle(int rowNum);
}
