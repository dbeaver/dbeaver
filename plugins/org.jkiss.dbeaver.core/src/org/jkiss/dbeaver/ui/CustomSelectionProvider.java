/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.viewers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite selection provider
 */
public class CustomSelectionProvider implements ISelectionProvider {

    private List<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();
    private ISelection selection;

    public CustomSelectionProvider()
    {
        selection = new StructuredSelection();
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
        return selection;
    }

    @Override
    public void setSelection(ISelection selection)
    {
        this.selection = selection;
        for (ISelectionChangedListener listener : listeners) {
            listener.selectionChanged(new SelectionChangedEvent(this, selection));
        }

    }
}
