package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.ui.*;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorInput;

import java.util.HashSet;
import java.util.Set;

public class OpenSQLEditorAction extends DataSourceAction
{
    static Log log = LogFactory.getLog(OpenSQLEditorAction.class);

    public OpenSQLEditorAction()
    {
        // The id is used to refer to the action in a menu or toolbar
        //setId(ICommandIds.CMD_OPEN_SQLEDITOR);
        // Associate the action with a pre-defined command, to allow key bindings.
        //setActionDefinitionId(ICommandIds.CMD_OPEN_SQLEDITOR);
        //setImageDescriptor(DBeaverActivator.getImageDescriptor("/icons/sql/sql_script.png"));
        //setText("Open SQL Editor");
    }

    public void run(IAction action)
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer(true);
        if (dataSourceContainer != null) {
            IFile tempFile = DBeaverCore.getInstance().makeTempFile(
                dataSourceContainer.getName(),
                "sql",
                new NullProgressMonitor());
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