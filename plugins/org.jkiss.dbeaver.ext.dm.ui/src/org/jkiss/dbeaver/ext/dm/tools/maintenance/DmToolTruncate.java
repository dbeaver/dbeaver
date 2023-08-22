package org.jkiss.dbeaver.ext.dm.tools.maintenance;

import java.util.Collection;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.tasks.DmTasks;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;

public class DmToolTruncate implements IUserInterfaceTool{

	@Override
	public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects)
			throws DBException {
		// TODO Auto-generated method stub
        TaskConfigurationWizardDialog.openNewTaskDialog(
                window,
                NavigatorUtils.getSelectedProject(),
                DmTasks.TASK_TABLE_TRUNCATE,
                new StructuredSelection(objects.toArray()));
	}

}
