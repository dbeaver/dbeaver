package org.jkiss.dbeaver.ext.dm.tools;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.dm.model.DmSchema;
import org.jkiss.dbeaver.ext.dm.tasks.DmImportSettings;
import org.jkiss.dbeaver.ext.dm.tasks.DmTasks;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractNativeImportExportWizard;
import org.jkiss.dbeaver.ui.UIUtils;

// DM 数据库导入向导
public class DmImportWizard extends AbstractNativeImportExportWizard<DmImportSettings, DmSchema>{

	 private DmImportWizardPageSettings settingsPage;
	
	public DmImportWizard(DBTTask task) {
		super(task);
	}
	
	public DmImportWizard(DmSchema schema) {
		super(Collections.singletonList(schema),"DM 导入数据");
		
	}
	
	@Override
	protected DmImportSettings createSettings() {
		// TODO Auto-generated method stub
		return new DmImportSettings();
	}

	@Override
	public String getTaskTypeId() {
		// TODO Auto-generated method stub
		return DmTasks.TASK_DATABASE_RESTORE;
	}

	@Override
	public void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state) {
		// TODO Auto-generated method stub
        settingsPage.saveState();

        getSettings().saveSettings(runnableContext, new TaskPreferenceStore(state));
	}

    /**@Override
    public boolean isExportWizard() {
        return false;
    }*/

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        settingsPage = new DmImportWizardPageSettings(this);
    }

    @Override
    public void addPages() {
        addTaskConfigPages();
        addPage(settingsPage);
        super.addPages();
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        if (page == settingsPage) {
            return null;
        }
        return super.getNextPage(page);
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        if (page == logPage) {
            return settingsPage;
        }
        return super.getPreviousPage(page);
    }

    @Override
    public void onSuccess(long workTime) {
        UIUtils.showMessageBox(
            getShell(),
            "数据还原",
            "还原'" + getObjectsName() + "'",
            SWT.ICON_INFORMATION);
    }

	
}
