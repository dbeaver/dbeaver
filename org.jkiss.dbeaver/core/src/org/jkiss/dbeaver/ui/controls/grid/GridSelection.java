package org.jkiss.dbeaver.ui.controls.grid;

import org.eclipse.jface.viewers.IStructuredSelection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GridSelection
 */
class GridSelection implements IStructuredSelection
{
    private GridControl grid;

    public GridSelection(GridControl grid)
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
