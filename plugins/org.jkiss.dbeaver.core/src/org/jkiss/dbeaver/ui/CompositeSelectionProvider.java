package org.jkiss.dbeaver.ui;

import org.eclipse.jface.viewers.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite selection provider
 */
public class CompositeSelectionProvider implements ISelectionProvider {

    private List<Viewer> viewers = new ArrayList<Viewer>();

    public void addViewer(Viewer viewer)
    {
        viewers.add(viewer);
    }

    public void addSelectionChangedListener(ISelectionChangedListener listener)
    {
        for (ISelectionProvider provider : viewers) {
            provider.addSelectionChangedListener(listener);
        }
    }

    public void removeSelectionChangedListener(ISelectionChangedListener listener)
    {
        for (ISelectionProvider provider : viewers) {
            provider.removeSelectionChangedListener(listener);
        }
    }

    public ISelection getSelection()
    {
        for (Viewer provider : viewers) {
            if (provider.getControl().isFocusControl()) {
                return provider.getSelection();
            }
        }
        return new StructuredSelection();
    }

    public void setSelection(ISelection selection)
    {
        for (Viewer provider : viewers) {
            if (provider.getControl().isFocusControl()) {
                provider.setSelection(selection);
                break;
            }
        }
    }
}
