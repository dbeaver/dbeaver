/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2019 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.postgresql.ui.config;


import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.ext.postgresql.model.plan.PostgreQueryPlaner;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;


public class PostgreExplainPlanConfigurator implements DBEObjectConfigurator<DBCQueryPlannerConfiguration> {

    // It is static as we want to save it between plan explains
    private static boolean analyse;

    @Override
    public DBCQueryPlannerConfiguration configureObject(DBRProgressMonitor monitor, Object container, DBCQueryPlannerConfiguration configuration) {
        return new UITask<DBCQueryPlannerConfiguration>() {
            @Override
            protected DBCQueryPlannerConfiguration runTask() {
                PlanConfigDialog dialog = new PlanConfigDialog();
                if (dialog.open() == IDialogConstants.OK_ID) {
                    configuration.getParameters().put(PostgreQueryPlaner.PARAM_ANALYSE, analyse);
                    return configuration;
                }
                return null;
            }
        }.execute();
    }

    private static class PlanConfigDialog extends BaseDialog {

        public PlanConfigDialog() {
            super(UIUtils.getActiveWorkbenchShell(), "PostgreSQL explain plan configuration", null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite dialogArea = super.createDialogArea(parent);
            Group settingsGroup = UIUtils.createControlGroup(dialogArea, "Settings", 2, GridData.FILL_BOTH, 0);
            UIUtils.createCheckbox(settingsGroup, "Perform ANALYSE", "Perform EXPLAIN ANALYSE. Otherwise will do simple EXPLAIN.\n" +
                "Note: ANALYSE may take a lot of time for big tables", analyse, 2).addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    analyse = ((Button)e.widget).getSelection();
                }
            });
            return dialogArea;
        }
    }

}
