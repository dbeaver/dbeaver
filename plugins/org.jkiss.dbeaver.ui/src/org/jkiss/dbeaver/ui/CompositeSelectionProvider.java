/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    private List<ISelectionChangedListener> listeners = new ArrayList<>();
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
