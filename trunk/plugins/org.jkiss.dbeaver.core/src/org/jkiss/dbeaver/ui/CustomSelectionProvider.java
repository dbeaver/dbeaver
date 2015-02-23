/*
 * Copyright (C) 2010-2015 Serge Rieder
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
