/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.viewers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite selection provider
 */
public class CustomSelectionProvider implements ISelectionProvider {

    private List<ISelectionChangedListener> listeners = new ArrayList<>();
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
