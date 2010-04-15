/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GridSelection
 */
class GridSelection implements IStructuredSelection
{
    private Spreadsheet grid;

    public GridSelection(Spreadsheet grid)
    {
        this.grid = grid;
    }

    public boolean isEmpty()
    {
        return grid.getSelection().isEmpty();
    }

    public Object getFirstElement()
    {
        return grid.getSelection().iterator().next();
    }

    public Iterator<GridPos> iterator()
    {
        return grid.getSelection().iterator();
    }

    public int size()
    {
        return grid.getSelection().size();
    }

    public Object[] toArray()
    {
        return grid.getSelection().toArray();
    }

    public List<Object> toList()
    {
        return new ArrayList<Object>(grid.getSelection());
    }

}
