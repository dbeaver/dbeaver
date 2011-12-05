/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISources;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.project.ScriptsHandlerImpl;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.registry.ProjectRegistry;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorControl;

public abstract class BaseSQLEditorHandler extends DataSourceHandler {

    static final Log log = LogFactory.getLog(BaseSQLEditorHandler.class);

    protected SQLEditorBase getActiveControl(ExecutionEvent event)
    {
        Object control = HandlerUtil.getVariable(event, ISources.ACTIVE_FOCUS_CONTROL_NAME);
        if (!(control instanceof Composite)) return null;
        for (Composite parent = ((Composite)control).getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof SQLEditorControl) {
                return ((SQLEditorControl) parent).getEditor();
            }
        }
        return null;
    }

    protected DBSDataSourceContainer getCurrentConnection(ExecutionEvent event)
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(event, false, false);
        final ProjectRegistry projectRegistry = DBeaverCore.getInstance().getProjectRegistry();
        IProject project = dataSourceContainer != null ? dataSourceContainer.getRegistry().getProject() : projectRegistry.getActiveProject();
        if (dataSourceContainer == null) {
            final DataSourceRegistry dataSourceRegistry = projectRegistry.getDataSourceRegistry(project);
            if (dataSourceRegistry.getDataSources().size() == 1) {
                dataSourceContainer = dataSourceRegistry.getDataSources().get(0);
            }
        }
        return dataSourceContainer;
    }

    protected IFolder getCurrentFolder(ExecutionEvent event)
    {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection != null && !selection.isEmpty() && selection instanceof IStructuredSelection) {
            final Object element = ((IStructuredSelection) selection).getFirstElement();
            if (element instanceof DBNResource && ((DBNResource)element).getResource() instanceof IFolder) {
                return (IFolder) ((DBNResource)element).getResource();
            }
        }
        return null;
    }

}