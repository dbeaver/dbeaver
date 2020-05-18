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
package org.jkiss.dbeaver.ext.postgresql.tools.fdw;

import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;


class PostgreFDWConfigWizardPageFinal extends ActiveWizardPage<PostgreFDWConfigWizard> {

    private static final Log log = Log.getLog(PostgreFDWConfigWizardPageFinal.class);
    private boolean activated;
    private Object sqlPanel;
    private String scriptText;

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
            Group settingsGroup = UIUtils.createControlGroup(composite, "Script", 1, GridData.FILL_BOTH, 0);

            Composite sqlPanelPH = new Composite(settingsGroup, SWT.NONE);
            sqlPanelPH.setLayoutData(new GridData(GridData.FILL_BOTH));
            sqlPanelPH.setLayout(new FillLayout());
            UIServiceSQL service = DBWorkbench.getService(UIServiceSQL.class);
            if (service != null) {
                try {
                    sqlPanel = service.createSQLPanel(
                        UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
                        sqlPanelPH,
                        getWizard(),
                        "FDW Script",
                        true,
                        "");
                } catch (DBException e) {
                    log.debug(e);
                    setErrorMessage(e.getMessage());
                }
            }
            Composite buttonsPanel = UIUtils.createComposite(settingsGroup, 2);
            buttonsPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            UIUtils.createDialogButton(buttonsPanel, "Copy", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    UIUtils.setClipboardContents(buttonsPanel.getDisplay(), TextTransfer.getInstance(), scriptText);
                }
            });
            UIUtils.createDialogButton(buttonsPanel, "Save ...", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    final File saveFile = DialogUtils.selectFileForSave(
                        buttonsPanel.getShell(), "Save SQL script", new String[]{"*.sql", "*.txt", "*", "*.*"}, null);
                    if (saveFile != null) {
                        try {
                            IOUtils.writeFileFromString(saveFile, scriptText);
                        } catch (IOException e1) {
                            DBWorkbench.getPlatformUI().showError("Save scritp to file", "Error saving script to file " + saveFile.getAbsolutePath(), e1);
                        }
                    }
                }
            });
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
        StringBuilder script = new StringBuilder();
        try {
            PostgreDataSource dataSource = getWizard().getDatabase().getDataSource();
            getWizard().getRunnableContext().run(true, true, monitor -> {
                try {
                    DBExecUtils.tryExecuteRecover(monitor, dataSource, param -> {
                        try {
                            monitor.beginTask("Generate FDW script", 2);
                            monitor.subTask("Read actions");
                            List<DBEPersistAction> actions = getWizard().generateScript(monitor);
                            monitor.subTask("Generate script");
                            script.append(
                                SQLUtils.generateScript(
                                    dataSource,
                                    actions.toArray(new DBEPersistAction[0]),
                                    false));
                            monitor.done();
                        } catch (DBException e) {
                            throw new InvocationTargetException(e);
                        }
                    });
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

        scriptText = script.toString();

        UIServiceSQL service = DBWorkbench.getService(UIServiceSQL.class);
        if (service != null) {
            service.setSQLPanelText(sqlPanel, scriptText);
        }
    }

}
