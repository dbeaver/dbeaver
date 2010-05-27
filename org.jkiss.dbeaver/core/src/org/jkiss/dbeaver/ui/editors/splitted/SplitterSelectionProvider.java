/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.splitted;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.IWorkbenchPart;

public class SplitterSelectionProvider implements IPostSelectionProvider {

    private ListenerList listeners = new ListenerList();
    private ListenerList postListeners = new ListenerList();
    private SplitterEditorPart splitterEditor;

    public SplitterSelectionProvider(SplitterEditorPart SplitterEditor) {
        Assert.isNotNull(SplitterEditor);
        this.splitterEditor = SplitterEditor;
    }

    /* (non-Javadoc)
     * Method declared on <code>ISelectionProvider</code>.
     */
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        listeners.add(listener);
    }

    public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
    	postListeners.add(listener);
	}

    public void fireSelectionChanged(final SelectionChangedEvent event) {
        Object[] listeners = this.listeners.getListeners();
        fireEventChange(event, listeners);
    }

    public void firePostSelectionChanged(final SelectionChangedEvent event) {
		Object[] listeners = postListeners.getListeners();
		fireEventChange(event, listeners);
	}

	private void fireEventChange(final SelectionChangedEvent event, Object[] listeners) {
		for (int i = 0; i < listeners.length; ++i) {
            final ISelectionChangedListener l = (ISelectionChangedListener) listeners[i];
            SafeRunner.run(new SafeRunnable() {
                public void run() {
                    l.selectionChanged(event);
                }
            });
        }
	}

    public SplitterEditorPart getSplitterEditor() {
        return splitterEditor;
    }

    /* (non-Javadoc)
     * Method declared on <code>ISelectionProvider</code>.
     */
    public ISelection getSelection() {
        IWorkbenchPart activePart = splitterEditor.getActivePart();
        if (activePart != null) {
            ISelectionProvider selectionProvider = activePart.getSite().getSelectionProvider();
            if (selectionProvider != null) {
				return selectionProvider.getSelection();
			}
        }
        return StructuredSelection.EMPTY;
    }

    /* (non-JavaDoc)
     * Method declaed on <code>ISelectionProvider</code>.
     */
    public void removeSelectionChangedListener(
            ISelectionChangedListener listener) {
        listeners.remove(listener);
    }

    public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
    	postListeners.remove(listener);
	}

	/* (non-Javadoc)
     * Method declared on <code>ISelectionProvider</code>.
     */
    public void setSelection(ISelection selection) {
        IWorkbenchPart activeEditor = splitterEditor.getActivePart();
        if (activeEditor != null) {
            ISelectionProvider selectionProvider = activeEditor.getSite().getSelectionProvider();
            if (selectionProvider != null) {
				selectionProvider.setSelection(selection);
			}
        }
    }
}