/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * SpreadsheetSelection
 */
class SpreadsheetSelection implements IStructuredSelection
{
    private Spreadsheet grid;
    private Collection<GridPos> selection;

    public SpreadsheetSelection(Spreadsheet grid)
    {
        this.grid = grid;
        this.selection = grid.getSelection();
    }

    Spreadsheet getGrid()
    {
        return grid;
    }

    @Override
    public boolean isEmpty()
    {
        return selection.isEmpty();
    }

    @Override
    public Object getFirstElement()
    {
        return selection.iterator().next();
    }

    @Override
    public Iterator<GridPos> iterator()
    {
        return selection.iterator();
    }

    @Override
    public int size()
    {
        return selection.size();
    }

    @Override
    public Object[] toArray()
    {
        return selection.toArray();
    }

    @Override
    public List<Object> toList()
    {
        return new ArrayList<Object>(selection);
    }

}
