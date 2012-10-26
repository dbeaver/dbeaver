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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

class CompareObjectsPageSettings extends ActiveWizardPage<CompareObjectsWizard> {

    private Table nodesTable;
    private Button skipSystemObjects;
    private Button compareLazyProperties;

    CompareObjectsPageSettings() {
        super("Compare objects");
        setTitle("Compare database objects");
        setDescription("Compare database settings");
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

        CompareObjectsSettings settings = getWizard().getSettings();
        {
            Group sourceSettings = new Group(composite, SWT.NONE);
            sourceSettings.setText("Objects");
            gl = new GridLayout(1, false);
            sourceSettings.setLayout(gl);
            sourceSettings.setLayoutData(new GridData(GridData.FILL_BOTH));

            nodesTable = new Table(sourceSettings, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
            nodesTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            nodesTable.setHeaderVisible(true);
            UIUtils.createTableColumn(nodesTable, SWT.LEFT, "Name");
            UIUtils.createTableColumn(nodesTable, SWT.LEFT, "Type");
            UIUtils.createTableColumn(nodesTable, SWT.LEFT, "Full qualified name");
            for (DBNDatabaseNode node : settings.getNodes()) {
                TableItem item = new TableItem(nodesTable, SWT.NONE);
                item.setImage(node.getNodeIconDefault());
                item.setText(0, node.getNodeName());
                item.setText(1, node.getNodeType());
                item.setText(2, node.getNodeFullName());
            }
        }

        {
            Group compareSettings = new Group(composite, SWT.NONE);
            compareSettings.setText(CoreMessages.dialog_export_wizard_settings_group_exporter);
            compareSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            compareSettings.setLayout(new GridLayout(1, false));

            skipSystemObjects = UIUtils.createCheckbox(compareSettings, "Skip system object", settings.isSkipSystemObjects());
            compareLazyProperties = UIUtils.createCheckbox(compareSettings, "Compare lazy properties", settings.isCompareLazyProperties());
        }
        
        setControl(composite);
    }

    @Override
    public void activatePage() {
        UIUtils.packColumns(nodesTable, true);
        updatePageCompletion();
    }

    @Override
    public void deactivatePage()
    {
        CompareObjectsSettings settings = getWizard().getSettings();
        settings.setSkipSystemObjects(skipSystemObjects.getSelection());
        settings.setCompareLazyProperties(compareLazyProperties.getSelection());
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return true;
    }
}