package org.jkiss.dbeaver.ext.sample.database;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;

import static org.jkiss.dbeaver.ext.sample.database.WorkbenchInitializerCreateSampleDatabase.*;

public class SampleDatabaseHandler extends AbstractHandler {
    @Override
    public Object execute(ExecutionEvent event) {
        DBPProject activeProject = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (activeProject == null || !activeProject.isRegistryLoaded()) {
            // No active project
            return null;
        }
        DBPDataSourceRegistry registry = activeProject.getDataSourceRegistry();
        Shell shell = UIUtils.getActiveWorkbenchShell();
        if (isSampleDatabaseExists(registry)) {
            UIUtils.showMessageBox(shell, SampleDatabaseMessages.dialog_already_created_title, SampleDatabaseMessages.dialog_already_created_description, SWT.ICON_WARNING);
            return null;
        }
        if (showCreateSampleDatabasePrompt(shell)) {
            createSampleDatabase(registry);
        }
        return null;
    }
}
