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
package org.jkiss.dbeaver.tools.sql.ui.wizard;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.tools.sql.SQLScriptExecuteSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

/**
 * SQL task settings page
 */
class SQLTaskPageSettings extends ActiveWizardPage<SQLTaskConfigurationWizard> {

    private Button ignoreErrorsCheck;
    private Button dumpQueryCheck;
    private Button autoCommitCheck;

    SQLTaskPageSettings() {
        super("SQL Script execute");
        setTitle("SQL Script execute settings");
        setDescription("Parameter for SQL script execute task");
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 1);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        SQLScriptExecuteSettings dtSettings = getWizard().getSettings();

        {
            Composite settingsGroup = UIUtils.createControlGroup(composite, "Script settings", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            ignoreErrorsCheck = UIUtils.createCheckbox(settingsGroup, "Ignore Errors", "", dtSettings.isIgnoreErrors(), 2);
            dumpQueryCheck = UIUtils.createCheckbox(settingsGroup, "Dump query results to log file", "", dtSettings.isDumpQueryResultsToLog(), 2);
        }

        {
            Composite settingsGroup = UIUtils.createControlGroup(composite, "Script processing", 2, GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            autoCommitCheck = UIUtils.createCheckbox(settingsGroup, "Auto-commit", "", dtSettings.isAutoCommit(), 2);
        }

        getWizard().createTaskSaveButtons(composite, true, 1);

        setControl(composite);
    }

    @Override
    public void activatePage() {
        updatePageCompletion();
    }

    @Override
    public void deactivatePage() {
    }

    void saveState() {
        SQLScriptExecuteSettings settings = getWizard().getSettings();
        settings.setIgnoreErrors(ignoreErrorsCheck.getSelection());
        settings.setDumpQueryResultsToLog(dumpQueryCheck.getSelection());
        settings.setAutoCommit(autoCommitCheck.getSelection());
    }

    @Override
    protected boolean determinePageCompletion() {
        return true;
    }

}