/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.sql;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDataSourceContainerProvider;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.dialogs.connection.SelectDataSourceDialog;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class OpenSQLEditorHandler extends AbstractHandler {

    static final Log log = LogFactory.getLog(OpenSQLEditorHandler.class);

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(event, false);
        if (dataSourceContainer != null) {
            IFile tempFile;
            try {
                tempFile = DBeaverCore.getInstance().makeTempFile(
                    VoidProgressMonitor.INSTANCE,
                    DBeaverCore.getInstance().getAutosaveFolder(VoidProgressMonitor.INSTANCE),
                    dataSourceContainer.getName(),
                    "sql");
            }
            catch (IOException e) {
                log.error(e);
                return null;
            }
            IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
            SQLEditorInput sqlInput = new SQLEditorInput(
                tempFile,
                dataSourceContainer,
                getNewScriptName(workbenchWindow.getWorkbench()));
            try {
                workbenchWindow.getActivePage().openEditor(
                    sqlInput,
                    SQLEditor.class.getName());
            } catch (Exception ex) {
                log.error("Could not  open SQL editor", ex);
            }
        }
        return null;
    }

    private String getNewScriptName(IWorkbench workbench)
    {
        // Collect all open script names
        Set<String> openScripts = new HashSet<String>();
        for (IWorkbenchWindow window : workbench.getWorkbenchWindows()) {
            for (IWorkbenchPage page : window.getPages()) {
                for (IEditorReference editorReference : page.getEditorReferences()) {
                    try {
                        IEditorInput input = editorReference.getEditorInput();
                        if (input instanceof SQLEditorInput) {
                            openScripts.add( ((SQLEditorInput)input).getScriptName() );
                        }
                    } catch (PartInitException e) {
                        // do nothing
                    }
                }
            }
        }
        for (int i = 1; ; i++) {
            String scriptName = "" + i;
            if (!openScripts.contains(scriptName)) {
                return scriptName;
            }
        }
    }

    protected DBSDataSourceContainer getDataSourceContainer(ExecutionEvent event, boolean chooseOnNoSelection)
    {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider) activePart).getDataSourceContainer();
        }
        if (activePart instanceof IDataSourceProvider) {
            DBPDataSource dataSource = ((IDataSourceProvider) activePart).getDataSource();
            return dataSource == null ? null : dataSource.getContainer();
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

}