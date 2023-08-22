package org.jkiss.dbeaver.ext.dm.tools;

import org.jkiss.dbeaver.ext.dm.DmDataSourceProvider;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.tasks.DmTasks;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskType;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigPanel;
import org.jkiss.dbeaver.tasks.ui.DBTTaskConfigurator;
import org.jkiss.dbeaver.tasks.ui.nativetool.NativeToolConfigPanel;
import org.jkiss.dbeaver.tasks.ui.wizard.TaskConfigurationWizard;

public class DmTaskConfigurator implements DBTTaskConfigurator{

	/**@Override
	public DBTTaskConfigPanel createInputConfigurator(DBRRunnableContext runnableContext, DBTTaskType taskType) {
		return new ConfigPanel(runnableContext, taskType);
	}*/

	@Override
	public TaskConfigurationWizard createTaskConfigWizard(DBTTask taskConfiguration) {
        switch (taskConfiguration.getType().getId()) {
        case DmTasks.TASK_DATABASE_BACKUP:
            return new DmExportWizard(taskConfiguration);
        case DmTasks.TASK_DATABASE_RESTORE:
            return new DmImportWizard(taskConfiguration);
    }
    return null;
	}
	
    private static class ConfigPanel extends NativeToolConfigPanel<DmSchema> {
        ConfigPanel(DBRRunnableContext runnableContext, DBTTaskType taskType) {
            super(runnableContext, taskType, DmSchema.class, DmDataSourceProvider.class);
        }
    }

}
