/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.utils.CommonUtils;


class PostgreRestoreWizardPageSettings extends PostgreWizardPageSettings<PostgreRestoreWizard>
{

    private TextWithOpenFile inputFileText;
    private Combo formatCombo;

    protected PostgreRestoreWizardPageSettings(PostgreRestoreWizard wizard)
    {
        super(wizard, "Settings");
        setTitle("Restore settings");
        setDescription("Database restore settings");
    }

    @Override
    public boolean isPageComplete()
    {
        return super.isPageComplete() && !CommonUtils.isEmpty(wizard.inputFile);
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Listener updateListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                updateState();
            }
        };

        Group formatGroup = UIUtils.createControlGroup(composite, "Settings", 2, GridData.FILL_HORIZONTAL, 0);
        formatCombo = UIUtils.createLabelCombo(formatGroup, "Format", SWT.DROP_DOWN | SWT.READ_ONLY);
        formatCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        for (PostgreBackupWizard.ExportFormat format : PostgreBackupWizard.ExportFormat.values()) {
            formatCombo.add(format.getTitle());
        }
        formatCombo.select(wizard.format.ordinal());
        formatCombo.addListener(SWT.Selection, updateListener);

        Group inputGroup = UIUtils.createControlGroup(composite, "Input", 2, GridData.FILL_HORIZONTAL, 0);
        UIUtils.createControlLabel(inputGroup, "Backup file");
        inputFileText = new TextWithOpenFile(inputGroup, "Choose backup file", new String[] {"*.backup","*"});
        inputFileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        inputFileText.getTextControl().addListener(SWT.Modify, updateListener);

        createSecurityGroup(composite);

        setControl(composite);
    }

    private void updateState()
    {
        wizard.format = PostgreBackupWizard.ExportFormat.values()[formatCombo.getSelectionIndex()];
        wizard.inputFile = inputFileText.getText();

        getContainer().updateButtons();
    }

}
