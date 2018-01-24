/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2010-2017 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mockdata;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorRegistry;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockDataWizardPageSettings extends ActiveWizardPage<MockDataExecuteWizard>
{
    private MockDataSettings mockDataSettings;

    private Button removeOldDataCheck;
    private Text rowsText;

    private PropertyTreeViewer propsEditor;
    private NamedPropertySource propertySource;
    private Map<String, NamedPropertySource> propertySourceMap = new HashMap<>();
    private Table columnsTable;
    private boolean firstInit = true;

    protected MockDataWizardPageSettings(MockDataExecuteWizard wizard, MockDataSettings mockDataSettings)
    {
        super(MockDataMessages.tools_mockdata_wizard_page_settings_page_name);
        setTitle(MockDataMessages.tools_mockdata_wizard_page_settings_page_name);
        setDescription((MockDataMessages.tools_mockdata_wizard_page_settings_page_description));
        this.mockDataSettings = mockDataSettings;
    }

    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        {
            SelectionListener changeListener = new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            updateState();
                        }
                    };

            Group settingsGroup = UIUtils.createControlGroup(
                    composite, MockDataMessages.tools_mockdata_wizard_page_settings_group_settings, 4, GridData.FILL_HORIZONTAL, 0);

            this.removeOldDataCheck = UIUtils.createLabelCheckbox(
                    settingsGroup,
                    MockDataMessages.tools_mockdata_wizard_page_settings_checkbox_remove_old_data,
                    mockDataSettings.isRemoveOldData());
            removeOldDataCheck.addSelectionListener(changeListener);
            removeOldDataCheck.setLayoutData(
                    new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            this.rowsText = UIUtils.createLabelText(
                    settingsGroup, "Rows", String.valueOf(mockDataSettings.getRowsNumber()), SWT.BORDER,
                    new GridData(110, SWT.DEFAULT));
            rowsText.addSelectionListener(changeListener);
            rowsText.addVerifyListener(UIUtils.getLongVerifyListener(rowsText));
            rowsText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    updateState();
                }
            });
        }

        {
            Group generatorsGroup = UIUtils.createControlGroup(composite, "Generators", 5, GridData.FILL_BOTH, 0);

            columnsTable = new Table(generatorsGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            GridData layoutData = new GridData(GridData.FILL_VERTICAL);
            layoutData.widthHint = 230;
            columnsTable.setLayoutData(layoutData);
            columnsTable.setHeaderVisible(true);
            columnsTable.setLinesVisible(true);

            TableColumn column = UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Column");
            column.setWidth(155);
            column = UIUtils.createTableColumn(columnsTable, SWT.LEFT, "Type");
            column.setWidth(75);

            columnsTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    saveGeneratorProperties(propertySource);
                    reloadProperties((DBSEntityAttribute) e.item.getData());
                }
            });

            propsEditor = new PropertyTreeViewer(generatorsGroup, SWT.BORDER);
            propsEditor.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        }

        setControl(composite);

    }

    @Override
    public void activatePage() {
        List<DBSDataManipulator> databaseObjects = getWizard().getDatabaseObjects();
        DBSDataManipulator dataManipulator = databaseObjects.iterator().next();
        DBSEntity dbsEntity = (DBSEntity) dataManipulator;
        try {
            Collection<? extends DBSEntityAttribute> attributes = dbsEntity.getAttributes(new VoidProgressMonitor());

            // init the generators properties
            if (firstInit) {
                firstInit = false;
                for (DBSEntityAttribute attribute : attributes) {
                    saveGeneratorProperties(getPropertySource(attribute));
                }

            }

            // populate columns table
            TableItem firstTableItem = null;
            for (DBSEntityAttribute attribute : attributes) {
                TableItem item = new TableItem(columnsTable, SWT.NONE);
                item.setData(attribute);
                item.setImage(DBeaverIcons.getImage(DBValueFormatting.getTypeImage(attribute)));
                item.setText(0, attribute.getName());
                item.setText(1, attribute.getDataKind().name());
                if (firstTableItem == null) {
                    firstTableItem = item;
                }
            }

            // select the first item
            columnsTable.select(0);
            // and notify the listeners
            Event event = new Event();
            event.widget = columnsTable;
            event.display = columnsTable.getDisplay();
            event.item = firstTableItem;
            event.type = SWT.Selection;
            columnsTable.notifyListeners(SWT.Selection, event);
        } catch (DBException e) {
            e.printStackTrace();
        }

        updatePageCompletion();
    }

    @Override
    public void deactivatePage() {
        saveGeneratorProperties(propertySource);
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }

    private void updateState() {
        mockDataSettings.setRemoveOldData(removeOldDataCheck.getSelection());
        mockDataSettings.setRowsNumber(Long.parseLong(rowsText.getText()));
    }

    private NamedPropertySource getPropertySource(DBSEntityAttribute attribute) {
        NamedPropertySource propertySource = propertySourceMap.get(attribute.getName());
        if (propertySource != null) {
            return propertySource;
        } else {
            MockGeneratorRegistry generatorRegistry = MockGeneratorRegistry.getInstance();
            List<DBSDataManipulator> databaseObjects = getWizard().getDatabaseObjects();
            DBSDataManipulator dataManipulator = databaseObjects.iterator().next();
            MockGeneratorDescriptor generatorDescriptor = generatorRegistry.findGenerator(dataManipulator.getDataSource(), attribute);
            propertySource = new NamedPropertySource(attribute.getName(), generatorDescriptor.getProperties(), null);
            propertySourceMap.put(attribute.getName(), propertySource);
            return propertySource;
        }
    }

    private void reloadProperties(DBSEntityAttribute attribute) {
        propertySource = getPropertySource(attribute);
        propsEditor.loadProperties(propertySource);
        propsEditor.setExpandMode(PropertyTreeViewer.ExpandMode.FIRST);
        propsEditor.expandAll();
    }

    private void saveGeneratorProperties(NamedPropertySource namedPropertySource) {
        if (namedPropertySource == null) {
            return;
        }
        this.mockDataSettings.setGeneratorProperties(namedPropertySource.getName(), namedPropertySource.getPropertiesWithDefaults());
    }

    private static class NamedPropertySource extends PropertySourceCustom {
        private final String name;
        public NamedPropertySource(String name, Collection<? extends DBPPropertyDescriptor> properties, Map<?, ?> values) {
            super(properties, values);
            this.name = name;
        }
        public String getName() { return name; }
    }
}
