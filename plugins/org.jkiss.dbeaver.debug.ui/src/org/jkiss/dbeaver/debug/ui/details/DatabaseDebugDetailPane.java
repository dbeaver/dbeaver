package org.jkiss.dbeaver.debug.ui.details;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.debug.ui.IDetailPane2;
import org.eclipse.debug.ui.IDetailPane3;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.dbeaver.debug.ui.DebugUI;
import org.jkiss.dbeaver.ui.ActionBars;
import org.jkiss.dbeaver.ui.Widgets;

public abstract class DatabaseDebugDetailPane<EDITOR extends DatabaseDebugDetailEditor> implements IDetailPane2, IDetailPane3 {

    private String name;
    private String description;
    private String id;

    private IWorkbenchPartSite partSite;

    private EDITOR editor;

    private Set<Integer> autoSaveProperties = new HashSet<>();

    private ListenerList<IPropertyListener> propertyListeners = new ListenerList<>();
    private Composite editorParent;

    public DatabaseDebugDetailPane(String name, String description, String id)
    {
        this.name = name;
        this.description = description;
        this.id = id;
    }

    @Override
    public String getID()
    {
        return id;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public void init(IWorkbenchPartSite partSite)
    {
        this.partSite = partSite;
    }

    @Override
    public Control createControl(Composite parent)
    {
        editorParent = Widgets.createComposite(parent, 1, 1, GridData.FILL_BOTH);
        editorParent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        editor = createEditor(editorParent);
        editor.setMnemonics(false);
        editor.addPropertyListener(new IPropertyListener() {
            @Override
            public void propertyChanged(Object source, int propId)
            {
                if (autoSaveProperties.contains(new Integer(propId))) {
                    try {
                        editor.doSave();
                        return;
                    } catch (CoreException e) {
                    }
                }
                firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
            }
        });
        return editor.createControl(editorParent);
    }

    protected abstract EDITOR createEditor(Composite parent);

    protected void registerAutosaveProperties(int[] autosave) {
        for (int i = 0; i < autosave.length; i++) {
            autoSaveProperties.add(new Integer(autosave[i]));
        }
    }

    protected void unregisterAutosaveProperties(int[] autosave) {
        for (int i = 0; i < autosave.length; i++) {
            autoSaveProperties.remove(new Integer(autosave[i]));
        }
    }

    @Override
    public ISelectionProvider getSelectionProvider()
    {
        return null;
    }

    @Override
    public boolean setFocus()
    {
        return false;
    }

    @Override
    public void display(IStructuredSelection selection)
    {
        // clear status line
        IStatusLineManager statusLine = ActionBars.extractStatusLineManager(partSite);
        if (statusLine != null) {
            statusLine.setErrorMessage(null);
        }
        EDITOR editor = getEditor();
        Object input = null;
        if (selection != null && selection.size() == 1) {
            input = selection.getFirstElement();
            // update even if the same in case attributes have changed
        }
        try {
            editor.setInput(input);
        } catch (CoreException e) {
            DebugUI.log(e.getStatus());
        }
    }

    protected EDITOR getEditor() {
        return editor;
    }

    @Override
    public boolean isDirty()
    {
        return editor != null && editor.isDirty();
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {
        IStatusLineManager statusLine = ActionBars.extractStatusLineManager(partSite);
        if (statusLine != null) {
            statusLine.setErrorMessage(null);
        }
        try {
            editor.doSave();
        } catch (CoreException e) {
            if (statusLine != null) {
                statusLine.setErrorMessage(e.getMessage());
            } else {
                DebugUI.log(e.getStatus());
            }
        }
    }

    @Override
    public void doSaveAs()
    {
        // do nothing
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }

    @Override
    public boolean isSaveOnCloseNeeded()
    {
        return isDirty() && editor.getStatus().isOK();
    }

    @Override
    public void addPropertyListener(IPropertyListener listener)
    {
        propertyListeners.add(listener);
    }

    @Override
    public void removePropertyListener(IPropertyListener listener)
    {
        propertyListeners.remove(listener);
    }

    protected void firePropertyChange(int property)
    {
        for (IPropertyListener listener : propertyListeners) {
            listener.propertyChanged(this, property);
        }
    }

    @Override
    public void dispose()
    {
        editor = null;
        partSite = null;
        propertyListeners.clear();
        autoSaveProperties.clear();
        editorParent.dispose();
    }

}
