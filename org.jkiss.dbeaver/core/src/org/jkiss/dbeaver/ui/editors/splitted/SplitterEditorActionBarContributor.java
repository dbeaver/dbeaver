package org.jkiss.dbeaver.ui.editors.splitted;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.EditorActionBarContributor;

public abstract class SplitterEditorActionBarContributor extends
        EditorActionBarContributor {
    protected SplitterEditorActionBarContributor() {
        super();
    }

    public void setActiveEditor(IEditorPart part) {
        IWorkbenchPart activeNestedEditor = null;
        if (part instanceof SplitterEditorPart) {
            activeNestedEditor = ((SplitterEditorPart) part).getActivePart();
        }
        setActivePage(activeNestedEditor);
    }

    public abstract void setActivePage(IWorkbenchPart activePart);
}