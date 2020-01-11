/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.oracle.ui.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.tasks.OracleScriptExecuteSettings;
import org.jkiss.dbeaver.ext.oracle.tasks.OracleTasks;
import org.jkiss.dbeaver.ext.oracle.ui.internal.OracleUIMessages;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.tasks.ui.nativetool.AbstractScriptExecuteWizard;

import java.util.Collections;
import java.util.Map;

class OracleScriptExecuteWizard extends AbstractScriptExecuteWizard<OracleScriptExecuteSettings, DBSObject, OracleDataSource> {

    private OracleScriptExecuteWizardPageSettings mainPage;

    OracleScriptExecuteWizard(DBTTask task) {
        super(task);
    }

    OracleScriptExecuteWizard(OracleDataSource oracleSchema) {
        super(Collections.singleton(oracleSchema), OracleUIMessages.tools_script_execute_wizard_page_name);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);

        this.mainPage = new OracleScriptExecuteWizardPageSettings(this);
    }

    @Override
    public String getTaskTypeId() {
        return OracleTasks.TASK_SCRIPT_EXECUTE;
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state) {
        mainPage.saveState();

        getSettings().saveSettings(runnableContext, new TaskPreferenceStore(state));
    }

    @Override
    protected OracleScriptExecuteSettings createSettings() {
        return new OracleScriptExecuteSettings();
    }

    @Override
    public void addPages() {
        addTaskConfigPages();
        addPage(mainPage);
        super.addPages();
    }

}
