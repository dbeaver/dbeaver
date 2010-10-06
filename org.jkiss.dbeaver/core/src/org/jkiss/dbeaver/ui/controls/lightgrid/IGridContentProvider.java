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

    public Object getElement(GridPos pos);
}
