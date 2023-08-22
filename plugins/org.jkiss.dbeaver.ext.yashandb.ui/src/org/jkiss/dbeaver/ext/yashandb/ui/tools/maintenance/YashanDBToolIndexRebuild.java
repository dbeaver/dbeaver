package org.jkiss.dbeaver.ext.yashandb.ui.tools.maintenance;

import java.util.Collection;

import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.yashandb.tasks.YashanDBTasks;
import org.jkiss.dbeaver.model.struct.DBSObject;

import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizardDialog;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.tools.IUserInterfaceTool;
/**
 * @Description:
 * @Author dengqh
 * @Date 2023/7/3 9:58
 */
public class YashanDBToolIndexRebuild implements IUserInterfaceTool {
	 @Override
	    public void execute(IWorkbenchWindow window, IWorkbenchPart activePart, Collection<DBSObject> objects) throws DBException {
	        TaskConfigurationWizardDialog.openNewTaskDialog(
	                window,
	                NavigatorUtils.getSelectedProject(),
	                YashanDBTasks.TASK_INDEX_REBUILD,
	                new StructuredSelection(objects.toArray()));
	    }

}
