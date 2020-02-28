/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.wizard;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tasks.ui.registry.TaskUIRegistry;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.Map;

/**
 * "Fake" wizard.
 * We need it because there is no wizard before user select some particular task type.
 * Once he does we "replace" this wizard with real one om wizard dialog.
 */
class TaskConfigurationWizardStub extends TaskConfigurationWizard {

    protected TaskConfigurationWizardStub() {
    }

    @Override
    protected String getDefaultWindowTitle() {
        return "Create a task";
    }

    @Override
    public String getTaskTypeId() {
        return null;
    }

    @Override
    public void saveTaskState(DBRRunnableContext runnableContext, DBTTask task, Map<String, Object> state) {

    }

    @Override
    public void addPages() {
        addPage(new TaskConfigurationWizardPageTask(null));
        addPage(new TaskConfigurationWizardPageSettings(null));
        addPage(new TaskConfigurationVoidPage());
    }

    private void addTaskWizardPages() {
/*
        if (wizard == null) {
            return;
        }
        wizard.addPages();
        for (IWizardPage page : wizard.getPages()) {
            addPage(page);
            page.setWizard(wizard);
        }
*/
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        return super.getNextPage(page);
    }

    @Override
    public boolean canFinish() {
        return false;
    }

    @Override
    public boolean performFinish() {
        return false;//wizard.performFinish();
    }

    public boolean isLastTaskPreconfigPage(IWizardPage page) {
        return page instanceof TaskConfigurationWizardPageSettings ||
            (page instanceof TaskConfigurationWizardPageTask &&
                ((TaskConfigurationWizardPageTask) page).getSelectedTaskType() != null &&
                !TaskUIRegistry.getInstance().supportsConfiguratorPage(((TaskConfigurationWizardPageTask) page).getSelectedTaskType()));

    }

    class TaskConfigurationVoidPage extends ActiveWizardPage
    {

        protected TaskConfigurationVoidPage() {
            super("Void page");
        }

        @Override
        public void createControl(Composite parent) {
            setControl(new Label(parent, SWT.NONE));
        }
    }

}