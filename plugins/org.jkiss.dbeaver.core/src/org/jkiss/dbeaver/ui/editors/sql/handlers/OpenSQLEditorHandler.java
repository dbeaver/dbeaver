/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.*;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.impl.project.ScriptsHandlerImpl;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;

import java.util.HashSet;
import java.util.Set;

public class OpenSQLEditorHandler extends DataSourceHandler {

    static final Log log = LogFactory.getLog(OpenSQLEditorHandler.class);

    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(event, false, false);
        if (dataSourceContainer != null) {
            IFile scriptFile;
            try {
                scriptFile = ScriptsHandlerImpl.createNewScript(dataSourceContainer.getRegistry().getProject());
            }
            catch (CoreException e) {
                log.error(e);
                return null;
            }

            IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
            SQLEditorInput sqlInput = new SQLEditorInput(
                scriptFile,
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

}