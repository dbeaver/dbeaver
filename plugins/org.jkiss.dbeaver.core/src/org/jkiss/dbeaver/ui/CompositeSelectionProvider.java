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
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Control;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite selection provider
 */
public class CompositeSelectionProvider implements ISelectionProvider {

    private List<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();
    private ISelectionProvider provider;
    private ISelection selection = StructuredSelection.EMPTY;

    public void trackProvider(final Control control, final ISelectionProvider selectionProvider)
    {
        control.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e)
            {
                setProvider(selectionProvider);
            }
        });
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
        ISelectionProvider cp = getProvider();
        if (cp != null) {
            return cp.getSelection();
        } else {
            return selection;
        }
    }

    @Override
    public void setSelection(ISelection selection)
    {
        ISelectionProvider cp = getProvider();
        if (cp != null) {
            cp.setSelection(selection);
        } else {
            this.selection = selection;
            if (!CommonUtils.isEmpty(listeners)) {
            	SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
            	for (ISelectionChangedListener listener : listeners) {
            		 listener.selectionChanged(event);
            	}
            }
        }
    }

    public ISelectionProvider getProvider()
    {
        return provider;
    }

    public void setProvider(ISelectionProvider newProvider)
    {
        if (this.provider != newProvider){
        	ISelection newSelection = null;
            if (!CommonUtils.isEmpty(listeners)) {
                if (this.provider != null){
                    for (ISelectionChangedListener listener : listeners) {
                         this.provider.removeSelectionChangedListener(listener);
                    }
                }

                if (newProvider != null) {
                    for (ISelectionChangedListener listener : listeners) {
                         newProvider.addSelectionChangedListener(listener);
                    }

	                newSelection = newProvider.getSelection();
                } else {
                	newSelection = this.selection;
                }
            }
            this.provider = newProvider;

            if (newSelection != null){
            	//force a selection change event propagation
            	setSelection(newSelection);
            }
        }
    }
}
