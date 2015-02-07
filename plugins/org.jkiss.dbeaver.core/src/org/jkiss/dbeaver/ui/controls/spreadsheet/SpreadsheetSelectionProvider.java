/*
 * Copyright (C) 2010-2014 Serge Rieder
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

import org.jkiss.dbeaver.core.Log;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * SpreadsheetSelectionProvider
 */
public class SpreadsheetSelectionProvider implements ISelectionProvider
{
    static final Log log = Log.getLog(SpreadsheetSelectionProvider.class);

    private Spreadsheet grid;
    private List<ISelectionChangedListener> listeners;

    public SpreadsheetSelectionProvider(Spreadsheet grid)
    {
        this.grid = grid;
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public ISelection getSelection()
    {
        return new SpreadsheetSelection(grid);
    }

    @Override
    public void setSelection(ISelection selection)
    {
        log.warn("Grid do not supports external selection changes");
    }

    void onSelectionChange(ISelection selection)
    {
        if (!CommonUtils.isEmpty(listeners)) {
            SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).selectionChanged(event);
            }
        }
    }
}
