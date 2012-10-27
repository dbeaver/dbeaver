/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.tools.compare;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

class CompareObjectsPageOutput extends ActiveWizardPage<CompareObjectsWizard> {

    private Button showOnlyDifference;
    private Combo reportTypeCombo;

    CompareObjectsPageOutput() {
        super("Compare objects");
        setTitle("Compare database objects");
        setDescription("Configuration of output report");
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

        final CompareObjectsSettings settings = getWizard().getSettings();

        {
            Group reportSettings = new Group(composite, SWT.NONE);
            reportSettings.setText("Report settings");
            gl = new GridLayout(1, false);
            reportSettings.setLayout(gl);
            reportSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            showOnlyDifference = UIUtils.createCheckbox(reportSettings, "Show only differences", settings.isShowOnlyDifferences());
            showOnlyDifference.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    settings.setShowOnlyDifferences(showOnlyDifference.getSelection());
                }
            });
        }

        {
            Group outputSettings = new Group(composite, SWT.NONE);
            outputSettings.setText("Output");
            gl = new GridLayout(2, false);
            outputSettings.setLayout(gl);
            outputSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(outputSettings, "Report destination");
            reportTypeCombo = new Combo(outputSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
            reportTypeCombo.add("Open in browser");
            reportTypeCombo.add("Save to file");
            reportTypeCombo.select(0);
        }

        setControl(composite);
    }

    @Override
    public void activatePage() {
        updatePageCompletion();
    }

    @Override
    public void deactivatePage()
    {
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return true;
    }
}