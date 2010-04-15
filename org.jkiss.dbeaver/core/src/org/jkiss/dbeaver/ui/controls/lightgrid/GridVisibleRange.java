/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid;

/**
     * Object holding the visible range
 */
public class GridVisibleRange {
    private GridItem[] items = new GridItem[0];
    private GridColumn[] columns = new GridColumn[0];

    /**
     * @return the current items shown
     */
    public GridItem[] getItems()
    {
        return items;
    }

    void setItems(GridItem[] items)
    {
        this.items = items;
    }

    /**
     * @return the current columns shown
     */
    public GridColumn[] getColumns()
    {
        return columns;
    }

    public void setColumns(GridColumn[] columns)
    {
        this.columns = columns;
    }
}
