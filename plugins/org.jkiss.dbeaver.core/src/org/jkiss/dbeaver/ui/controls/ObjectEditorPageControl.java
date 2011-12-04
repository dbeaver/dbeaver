/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseEditor;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.struct.DBSObject;

public class ObjectEditorPageControl extends ProgressPageControl {

    //private Button saveChangesButton;
    private IDatabaseEditor workbenchPart;
    private IPropertyListener propertyListener;
    //private ToolBarManager objectEditToolbarManager;

    public ObjectEditorPageControl(Composite parent, int style, IDatabaseEditor workbenchPart)
    {
        super(parent, style);
        this.workbenchPart = workbenchPart;

//        if (isObjectEditable()) {
//            propertyListener = new IPropertyListener() {
//                public void propertyChanged(Object source, int propId)
//                {
//                    if (propId == IEditorPart.PROP_DIRTY) {
//                        boolean dirty = ((IEditorPart) source).isDirty();
//                        saveChangesButton.setEnabled(dirty);
//                        //viewChangesButton.setEnabled(dirty);
//                        //resetChangesButton.setEnabled(dirty);
//                    }
//                }
//            };
//            getMainEditorPart().addPropertyListener(propertyListener);
//        }
    }

    @Override
    public void dispose()
    {
/*
        if (objectEditToolbarManager != null) {
            objectEditToolbarManager.dispose();
            objectEditToolbarManager = null;
        }
*/
        if (propertyListener != null) {
            getMainEditorPart().removePropertyListener(propertyListener);
            propertyListener = null;
        }
        super.dispose();
    }

    public IDatabaseEditor getEditorPart()
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
        DBSObject databaseObject = getEditorPart().getEditorInput().getDatabaseObject();
        return databaseObject != null && DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(databaseObject.getClass(), DBEObjectManager.class) != null;
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


/*
    @Override
    protected Composite createProgressPanel(Composite container) {
        Composite panel = super.createProgressPanel(container);

        DBECommandContext commandContext = getEditorPart().getEditorInput().getCommandContext();
        if (commandContext == null || !isObjectEditable()) {
            return panel;
        }

        final Composite toolsPanel = UIUtils.createPlaceholder(panel, 1);

        ToolBar toolBar = new ToolBar(toolsPanel, SWT.FLAT | SWT.HORIZONTAL);

        objectEditToolbarManager = new ToolBarManager(toolBar);
        objectEditToolbarManager.add(ActionUtils.makeCommandContribution(
            DBeaverCore.getInstance().getWorkbench(),
            ICommandIds.CMD_OBJECT_CREATE));
        objectEditToolbarManager.add(ActionUtils.makeCommandContribution(
            DBeaverCore.getInstance().getWorkbench(),
            ICommandIds.CMD_OBJECT_DELETE));
        objectEditToolbarManager.add(ActionUtils.makeCommandContribution(
            DBeaverCore.getInstance().getWorkbench(),
            IWorkbenchCommandConstants.FILE_SAVE));
        objectEditToolbarManager.update(true);
        //objectEditToolbarManager.createControl(toolsPanel);

//        saveChangesButton = new Button(toolsPanel, SWT.FLAT | SWT.PUSH);
//        saveChangesButton.setText("Save / Preview");
//        saveChangesButton.setImage(DBIcon.SAVE_TO_DATABASE.getImage());
//        saveChangesButton.setToolTipText("Persist all changes");
//        saveChangesButton.setEnabled(false);
//        saveChangesButton.addSelectionListener(new SelectionAdapter() {
//            @Override
//            public void widgetSelected(SelectionEvent e) {
//                try {
//                    workbenchPart.getSite().getWorkbenchWindow().run(true, true, new IRunnableWithProgress() {
//                        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
//                        {
//                            getMainEditorPart().doSave(monitor);
//                        }
//                    });
//                } catch (InvocationTargetException e1) {
//                    UIUtils.showErrorDialog(null, "Save DB object", "Can't save database object", e1.getTargetException());
//                } catch (InterruptedException e1) {
//                    // do nothing
//                }
//            }
//        });

        return panel;
    }
*/

}
