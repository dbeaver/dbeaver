/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLDatabaseExportInfo;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLExportSettings;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLTasks;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractImportExportWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Map;

class MySQLExportWizard extends AbstractImportExportWizard<MySQLExportSettings, MySQLDatabaseExportInfo> implements IExportWizard {

    private MySQLExportWizardPageObjects objectsPage;
    private MySQLExportWizardPageSettings settingsPage;

    MySQLExportWizard(Collection<DBSObject> objects) {
        super(objects, MySQLUIMessages.tools_db_export_wizard_task_name);
        getSettings().fillExportObjectsFromInput();
    }

    MySQLExportWizard(DBTTask task) {
        super(task);
    }

    @Override
    protected MySQLExportSettings createSettings() {
        return new MySQLExportSettings();
    }

    @Override
    public String getTaskTypeId() {
        return MySQLTasks.TASK_DATABASE_BACKUP;
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
        objectsPage = new MySQLExportWizardPageObjects(this);
        settingsPage = new MySQLExportWizardPageSettings(this);
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
            MySQLUIMessages.tools_db_export_wizard_title,
            CommonUtils.truncateString(NLS.bind(MySQLUIMessages.tools_db_export_wizard_message_export_completed, getObjectsName()), 255),
            SWT.ICON_INFORMATION);
        UIUtils.launchProgram(getSettings().getOutputFolder().getAbsolutePath());
	}

    @Override
    public boolean isVerbose()
    {
        return true;
    }

}
