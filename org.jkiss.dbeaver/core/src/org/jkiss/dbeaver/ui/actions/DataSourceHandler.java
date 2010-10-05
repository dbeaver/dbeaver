/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.dialogs.connection.SelectDataSourceDialog;
import org.jkiss.dbeaver.utils.ViewUtils;

public abstract class DataSourceHandler extends AbstractHandler {

    protected DBSDataSourceContainer getDataSourceContainer(ExecutionEvent event, boolean useEditor, boolean chooseOnNoSelection)
    {
        if (useEditor) {
            IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (editor != null) {
                DBSDataSourceContainer container = getDataSourceContainer(editor);
                if (container != null) {
                    return container;
                }
            }
            return null;
        }
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        DBSDataSourceContainer container = getDataSourceContainer(activePart);
        if (container != null) {
            return container;
        }
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof IStructuredSelection) {
            DBSObject selectedObject = ViewUtils.getSelectedObject((IStructuredSelection) selection);
            if (selectedObject instanceof DBSDataSourceContainer) {
                return (DBSDataSourceContainer)selectedObject;
            } else if (selectedObject != null) {
                DBPDataSource dataSource = selectedObject.getDataSource();
                return dataSource == null ? null : dataSource.getContainer();
            }
        }
        if (chooseOnNoSelection) {
            return SelectDataSourceDialog.selectDataSource(HandlerUtil.getActiveShell(event));
        }
        return null;
    }

    public static DBSDataSourceContainer getDataSourceContainer(IWorkbenchPart activePart)
    {
        if (activePart instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider) activePart).getDataSourceContainer();
        }
        if (activePart instanceof IDataSourceProvider) {
            DBPDataSource dataSource = ((IDataSourceProvider) activePart).getDataSource();
            return dataSource == null ? null : dataSource.getContainer();
        }
        return null;
    }
}