/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.ext.ui.IDatabaseObjectEditor;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;

public class ObjectEditorHandler {

    private Button saveChangesButton;
    private IDatabaseObjectEditor workbenchPart;
    private IPropertyListener propertyListener;
/*
    private Button viewChangesButton;
    private Button resetChangesButton;
*/

    public ObjectEditorHandler(IDatabaseObjectEditor workbenchPart)
    {
        this.workbenchPart = workbenchPart;

        if (isObjectEditable()) {
            propertyListener = new IPropertyListener() {
                public void propertyChanged(Object source, int propId)
                {
                    if (propId == IEditorPart.PROP_DIRTY) {
                        boolean dirty = ((IEditorPart) source).isDirty();
                        saveChangesButton.setEnabled(dirty);
                        //viewChangesButton.setEnabled(dirty);
                        //resetChangesButton.setEnabled(dirty);
                    }
                }
            };
            getMainEditorPart().addPropertyListener(propertyListener);
        }
    }

    public IDatabaseObjectEditor getPart()
    {
        return workbenchPart;
    }

    public void dispose()
    {
        if (propertyListener != null) {
            getMainEditorPart().removePropertyListener(propertyListener);
            propertyListener = null;
        }
    }

    public IDatabaseObjectEditor getEditorPart()
    {
        return workbenchPart;
    }

    public boolean isObjectEditable()
    {
        DBPDataSource dataSource = getEditorPart().getEditorInput().getDataSource();
        if (dataSource == null) {
            return false;
        }
        if (dataSource.getInfo().isReadOnlyMetaData()) {
            return false;
        }
        return getEditorPart().getEditorInput().getObjectEditor() != null;
    }

    private IEditorPart getMainEditorPart()
    {
        IWorkbenchPartSite site = workbenchPart.getSite();
        if (site instanceof MultiPageEditorSite) {
            return ((MultiPageEditorSite)site).getMultiPageEditor();
        } else {
            return workbenchPart;
        }
    }

    public Composite createEditorControls(Composite panel) {
        DBEObjectCommander objectCommander = getEditorPart().getEditorInput().getObjectCommander();
        if (objectCommander != null && isObjectEditable()) {
            saveChangesButton = new Button(panel, SWT.PUSH);
            saveChangesButton.setText("Save / Preview");
            saveChangesButton.setImage(DBIcon.SAVE_TO_DATABASE.getImage());
            saveChangesButton.setToolTipText("Persist all changes");
            saveChangesButton.setEnabled(false);
            saveChangesButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    try {
                        workbenchPart.getSite().getWorkbenchWindow().run(true, true, new IRunnableWithProgress() {
                            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                            {
                                getMainEditorPart().doSave(monitor);
                            }
                        });
                    } catch (InvocationTargetException e1) {
                        UIUtils.showErrorDialog(null, "Save DB object", null, e1.getTargetException());
                    } catch (InterruptedException e1) {
                        // do nothing
                    }
                }
            });
        }
        return panel;
    }

}
