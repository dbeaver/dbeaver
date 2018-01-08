/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;

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
    public void disposeControl()
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
        super.disposeControl();
    }

    public IDatabaseEditor getEditorPart()
    {
        return workbenchPart;
    }

    public boolean isObjectEditable()
    {
        DBCExecutionContext context = getEditorPart().getEditorInput().getExecutionContext();
        if (context == null) {
            return false;
        }
        if (context.getDataSource().getInfo().isReadOnlyMetaData()) {
            return false;
        }
        DBSObject databaseObject = getEditorPart().getEditorInput().getDatabaseObject();
        return databaseObject != null && EntityEditorsRegistry.getInstance().getObjectManager(databaseObject.getClass(), DBEObjectManager.class) != null;
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
            CoreCommands.CMD_OBJECT_CREATE));
        objectEditToolbarManager.add(ActionUtils.makeCommandContribution(
            DBeaverCore.getInstance().getWorkbench(),
            CoreCommands.CMD_OBJECT_DELETE));
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

    @Override
    protected void fillCustomActions(IContributionManager contributionManager) {
        super.fillCustomActions(contributionManager);
    }
}
