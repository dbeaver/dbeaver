/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package  org.jkiss.dbeaver.ui.controls.lightgrid;

import org.eclipse.jface.viewers.IStructuredContentProvider;

public interface IGridContentProvider extends IStructuredContentProvider {

    /**
     * Row count
     * @return row count
     */
    public GridPos getSize();

    /**
     * Gets element by position
     * @param pos grid position
     * @return element (may be null)
     */
    public Object getElement(GridPos pos);

    /**
     * Updates grid column properties.
     * Invoked once right after grid columns initialization.
     * @param column grid column
     */
    public void updateColumn(GridColumn column);

}
