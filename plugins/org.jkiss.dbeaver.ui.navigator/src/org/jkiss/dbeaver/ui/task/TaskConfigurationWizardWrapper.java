/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.task;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.BaseWizard;

class TaskConfigurationWizardWrapper<WIZARD extends TaskConfigurationWizard> extends BaseWizard {

    @Nullable
    private WIZARD wizard;

    protected TaskConfigurationWizardWrapper() {
        this.wizard = wizard;
    }

    @Override
    public void addPages() {
        if (wizard == null) {
            addPage(new TaskConfigurationCreatePage());
            addPage(new TaskConfigurationVoidPage());
        } else {
            wizard.setContainer(getContainer());
            addTaskWizardPages();
        }
    }

    private void addTaskWizardPages() {
        if (wizard == null) {
            return;
        }
        wizard.addPages();
        for (IWizardPage page : wizard.getPages()) {
            addPage(page);
            page.setWizard(wizard);
        }
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        return super.getNextPage(page);
    }

    @Override
    public boolean canFinish() {
        if (wizard != null && wizard.canFinish()) {
            return true;
        }
        return false;
    }

    @Override
    public boolean performFinish() {
        return wizard.performFinish();
    }

    public WIZARD getTaskWizard() {
        return wizard;
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