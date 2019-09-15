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
package org.jkiss.dbeaver.ext.postgresql.tools.fdw;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.lang.reflect.InvocationTargetException;


class PostgreFDWConfigWizardPageFinal extends ActiveWizardPage<PostgreFDWConfigWizard> {

    private static final Log log = Log.getLog(PostgreFDWConfigWizardPageFinal.class);
    private boolean activated;
    private Object sqlPanel;

    protected PostgreFDWConfigWizardPageFinal(PostgreFDWConfigWizard wizard)
    {
        super("Script");
        setTitle("Foreign wrappers mapping SQL script");
        setDescription("Preview script and perform install");
        setWizard(wizard);
    }

    @Override
    public boolean isPageComplete()
    {
        return activated && getErrorMessage() == null;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Group settingsGroup = UIUtils.createControlGroup(composite, "Script", 2, GridData.FILL_BOTH, 0);
            settingsGroup.setLayout(new FillLayout());
            UIServiceSQL service = DBWorkbench.getService(UIServiceSQL.class);
            if (service != null) {
                try {
                    sqlPanel = service.createSQLPanel(
                        UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
                        settingsGroup,
                        getWizard(),
                        "FDW Script",
                        true,
                        "");
                } catch (DBException e) {
                    log.debug(e);
                    setErrorMessage(e.getMessage());
                }
            }
        }


        setControl(composite);
    }

    @Override
    public void activatePage() {
        if (!activated) {
            activated = true;
        }
        generateScript();
        super.activatePage();
    }

    private void generateScript() {
        // Fill FDW list
        try {
            getWizard().getRunnableContext().run(false, true, monitor -> {
                try {
                    throw new DBCException("Not implemented yet");
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            log.debug(e.getTargetException());
            setErrorMessage(e.getTargetException().getMessage());
            return;
        } catch (InterruptedException e) {
            return;
        }
        setErrorMessage(null);

        String sql = "CREATE FOREIGN SERVER";

        UIServiceSQL service = DBWorkbench.getService(UIServiceSQL.class);
        if (service != null) {
            service.setSQLPanelText(sqlPanel, sql);
        }
    }

}
