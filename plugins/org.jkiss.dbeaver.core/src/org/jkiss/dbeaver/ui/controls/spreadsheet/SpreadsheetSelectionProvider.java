/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.spreadsheet;

import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import java.util.List;

/**
 * SpreadsheetSelectionProvider
 */
public class SpreadsheetSelectionProvider implements ISelectionProvider
{
    static final Log log = LogFactory.getLog(SpreadsheetSelectionProvider.class);

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
