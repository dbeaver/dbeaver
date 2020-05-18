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

import org.eclipse.jface.wizard.IWizardPage;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLScriptExecuteSettings;
import org.jkiss.dbeaver.ext.mysql.tasks.MySQLTasks;
import org.jkiss.dbeaver.ext.mysql.ui.internal.MySQLUIMessages;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractScriptExecuteWizard;

import java.util.Collections;
import java.util.Map;

class MySQLScriptExecuteWizard extends AbstractScriptExecuteWizard<MySQLScriptExecuteSettings, MySQLCatalog, MySQLCatalog> {

    private MySQLScriptExecuteWizardPageSettings settingsPage = new MySQLScriptExecuteWizardPageSettings(this);

    MySQLScriptExecuteWizard(MySQLCatalog catalog, boolean isImport) {
        super(Collections.singleton(catalog), isImport ? MySQLUIMessages.tools_script_execute_wizard_db_import : MySQLUIMessages.tools_script_execute_wizard_execute_script);
        this.getSettings().setImport(isImport);
    }

    MySQLScriptExecuteWizard(DBTTask task, boolean isImport) {
        super(task);
        this.getSettings().setImport(isImport);
    }

    @Override
    public String getTaskTypeId() {
        return getSettings().isImport() ? MySQLTasks.TASK_DATABASE_RESTORE : MySQLTasks.TASK_SCRIPT_EXECUTE;
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state) {
        settingsPage.saveState();
        getSettings().saveSettings(runnableContext, new TaskPreferenceStore(state));
    }

    public MySQLScriptExecuteSettings.LogLevel getLogLevel() {
        return getSettings().getLogLevel();
    }

    public boolean isImport() {
        return getSettings().isImport();
    }

    @Override
    public boolean isVerbose() {
        return getSettings().isVerbose();
    }

    @Override
    protected MySQLScriptExecuteSettings createSettings() {
        return new MySQLScriptExecuteSettings();
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

}
