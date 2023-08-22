package org.jkiss.dbeaver.ext.dm.tools;

import java.util.Collection;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.dm.tasks.DmExportSettings;
import org.jkiss.dbeaver.ext.dm.tasks.DmSchemaExportInfo;
import org.jkiss.dbeaver.ext.dm.tasks.DmTasks;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractNativeImportExportWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

class DmExportWizard extends AbstractNativeImportExportWizard<DmExportSettings, DmSchemaExportInfo> implements IExportWizard {

    private DmExportWizardPageObjects objectsPage;
    private DmExportWizardPageSettings settingsPage;

    DmExportWizard(Collection<DBSObject> objects) {
        super(objects, "DM数据库备份"); // 使用DM task来判断是否是DM数据库
        getSettings().fillExportObjectsFromInput();

    }

    DmExportWizard(DBTTask task) {
        super(task);
    }

    @Override
    protected DmExportSettings createSettings() {
        return new DmExportSettings();
    }

    @Override
    public String getTaskTypeId() {
        return DmTasks.TASK_DATABASE_BACKUP;
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state) {
        objectsPage.saveState();
        settingsPage.saveState();
        getSettings().saveSettings(runnableContext, new TaskPreferenceStore(state));
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        objectsPage = new DmExportWizardPageObjects(this);
        settingsPage = new DmExportWizardPageSettings(this);
    }

    @Override
    public void addPages() {
        addTaskConfigPages();
        addPage(objectsPage);
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
            "备份",
            CommonUtils.truncateString(NLS.bind("Schema \"{0}\" export completed", getObjectsName()), 255),
            SWT.ICON_INFORMATION);
        UIUtils.launchProgram(getSettings().getOutputFolderPattern());
	}

    @Override
    public boolean isVerbose()
    {
        return true;
    }

}
