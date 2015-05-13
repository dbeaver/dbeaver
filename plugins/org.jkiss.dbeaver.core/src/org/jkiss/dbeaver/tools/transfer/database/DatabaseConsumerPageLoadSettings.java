/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Spinner;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

public class DatabaseConsumerPageLoadSettings extends ActiveWizardPage<DataTransferWizard> {

    public DatabaseConsumerPageLoadSettings() {
        super("Data load");
        setTitle("Data load settings");
        setDescription("Configuration of table data load");
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);

        {
            Group performanceSettings = UIUtils.createControlGroup(composite, "Performance", 4, GridData.FILL_HORIZONTAL, 0);

            final Button newConnectionCheckbox = UIUtils.createLabelCheckbox(
                performanceSettings,
                CoreMessages.data_transfer_wizard_output_checkbox_new_connection,
                settings.isOpenNewConnections());
            newConnectionCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOpenNewConnections(newConnectionCheckbox.getSelection());
                }
            });
            newConnectionCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            final Button useTransactionsCheck = UIUtils.createLabelCheckbox(performanceSettings, "Use transactions", settings.isUseTransactions());
            useTransactionsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    settings.setUseTransactions(useTransactionsCheck.getSelection());
                }
            });
            useTransactionsCheck.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            final Spinner commitAfterEdit = UIUtils.createLabelSpinner(performanceSettings, "Commit after insert of ", settings.getCommitAfterRows(), 1, Integer.MAX_VALUE);
            commitAfterEdit.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    settings.setCommitAfterRows(commitAfterEdit.getSelection());
                }
            });
            commitAfterEdit.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));
        }

        {
            Group generalSettings = UIUtils.createControlGroup(composite, "General", 4, GridData.FILL_HORIZONTAL, 0);
            final Button showTableCheckbox = UIUtils.createLabelCheckbox(generalSettings, "Open table editor on finish", settings.isOpenTableOnFinish());
            showTableCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOpenTableOnFinish(showTableCheckbox.getSelection());
                }
            });
            showTableCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));
        }

        setControl(composite);
    }

    @Override
    public void activatePage()
    {
        final DatabaseConsumerSettings settings = getWizard().getPageSettings(this, DatabaseConsumerSettings.class);

        updatePageCompletion();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return true;
    }

}