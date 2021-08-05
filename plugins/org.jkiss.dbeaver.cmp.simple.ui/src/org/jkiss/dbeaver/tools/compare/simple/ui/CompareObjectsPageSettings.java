/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.compare.simple.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.tools.compare.simple.CompareObjectsSettings;
import org.jkiss.dbeaver.tools.compare.simple.ui.internal.CompareUIMessages;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

class CompareObjectsPageSettings extends ActiveWizardPage<CompareObjectsWizard> {

    private Table nodesTable;
    private Button skipSystemObjects;
    private Button compareLazyProperties;
    private Button compareOnlyStructure;
    private Button compareScriptProperties;

    CompareObjectsPageSettings() {
        super(CompareUIMessages.compare_objects_page_settings_page);
        setTitle(CompareUIMessages.compare_objects_page_settings_title);
        setDescription(CompareUIMessages.compare_objects_page_settings_description);
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
            Group sourceSettings = new Group(composite, SWT.NONE);
            sourceSettings.setText(CompareUIMessages.compare_objects_page_settings_group_objects);
            gl = new GridLayout(1, false);
            sourceSettings.setLayout(gl);
            sourceSettings.setLayoutData(new GridData(GridData.FILL_BOTH));

            nodesTable = new Table(sourceSettings, SWT.SINGLE | SWT.FULL_SELECTION | SWT.BORDER);
            nodesTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            nodesTable.setHeaderVisible(true);
            UIUtils.createTableColumn(nodesTable, SWT.LEFT, CompareUIMessages.compare_objects_page_settings_nodes_column_name);
            UIUtils.createTableColumn(nodesTable, SWT.LEFT, CompareUIMessages.compare_objects_page_settings_nodes_column_type);
            UIUtils.createTableColumn(nodesTable, SWT.LEFT, CompareUIMessages.compare_objects_page_settings_nodes_column_full_name);
            for (DBNDatabaseNode node : settings.getNodes()) {
                TableItem item = new TableItem(nodesTable, SWT.NONE);
                item.setImage(DBeaverIcons.getImage(node.getNodeIconDefault()));
                item.setText(0, node.getNodeName());
                item.setText(1, node.getNodeType());
                item.setText(2, node.getNodeFullName());
            }
        }

        {
            Group compareSettings = new Group(composite, SWT.NONE);
            compareSettings.setText(CompareUIMessages.compare_objects_page_settings_group_settings);
            compareSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            compareSettings.setLayout(new GridLayout(1, false));

            skipSystemObjects = UIUtils.createCheckbox(compareSettings, CompareUIMessages.compare_objects_page_settings_checkbox_skip_objects, settings.isSkipSystemObjects());
            skipSystemObjects.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    settings.setSkipSystemObjects(skipSystemObjects.getSelection());
                }
            });
            compareLazyProperties = UIUtils.createCheckbox(compareSettings, CompareUIMessages.compare_objects_page_settings_checkbox_compare_preporties, settings.isCompareLazyProperties());
            compareLazyProperties.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    settings.setCompareLazyProperties(compareLazyProperties.getSelection());
                }
            });
            compareOnlyStructure = UIUtils.createCheckbox(compareSettings, CompareUIMessages.compare_objects_page_settings_checkbox_compare_structure, settings.isCompareOnlyStructure());
            compareOnlyStructure.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    settings.setCompareOnlyStructure(compareOnlyStructure.getSelection());
                }
            });
            compareScriptProperties = UIUtils.createCheckbox(compareSettings, CompareUIMessages.compare_objects_page_settings_checkbox_scripts, settings.isCompareScripts());
            compareScriptProperties.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    settings.setCompareScripts(compareScriptProperties.getSelection());
                }
            });
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
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return true;
    }
}