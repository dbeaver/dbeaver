/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.spreadsheet;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import java.util.List;

/**
 * GridSelectionProvider
 */
public class GridSelectionProvider implements ISelectionProvider
{
    static Log log = LogFactory.getLog(GridSelectionProvider.class);

    private Spreadsheet grid;
    private List<ISelectionChangedListener> listeners;

    public GridSelectionProvider(Spreadsheet grid)
    {
        this.grid = grid;
    }

    public void addSelectionChangedListener(ISelectionChangedListener listener)
    {
        listeners.add(listener);
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener)
    {
        listeners.remove(listener);
    }

    public ISelection getSelection()
    {
        return new GridSelection(grid);
    }

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
