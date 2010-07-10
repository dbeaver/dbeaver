/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;

import java.util.HashSet;
import java.util.Set;
import java.io.IOException;

public class OpenSQLEditorAction extends DataSourceAction
{
    static Log log = LogFactory.getLog(OpenSQLEditorAction.class);

    public void run(IAction action)
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(true);
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
                return;
            }
            SQLEditorInput sqlInput = new SQLEditorInput(
                tempFile,
                dataSourceContainer,
                getNewScriptName(getWindow().getWorkbench()));
            try {
                getWindow().getActivePage().openEditor(
                    sqlInput,
                    SQLEditor.class.getName());
            } catch (Exception ex) {
                log.error("Can't open editor", ex);
            }
        }
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