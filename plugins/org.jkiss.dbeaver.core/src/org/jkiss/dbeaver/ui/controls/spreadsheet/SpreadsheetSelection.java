/*
 * Copyright (C) 2010-2013 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
