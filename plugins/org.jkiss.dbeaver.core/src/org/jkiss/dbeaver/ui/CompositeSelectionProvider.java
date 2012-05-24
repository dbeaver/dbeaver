/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.widgets.Composite;
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

    public void trackViewer(final Control control, final Viewer viewer)
    {
        control.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e)
            {
                setProvider(viewer);
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
        if (provider != null) {
            return provider.getSelection();
        } else {
            return selection;
        }
    }

    @Override
    public void setSelection(ISelection selection)
    {
        if (provider != null) {
            provider.setSelection(selection);
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
