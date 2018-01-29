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

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mockdata.MockDataSettings.AttributeGeneratorProperties;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class MockDataWizardPageSettings extends ActiveWizardPage<MockDataExecuteWizard>
{
    private MockDataSettings mockDataSettings;

    private CLabel noGeneratorInfoLabel;
    private Button removeOldDataCheck;
    private Text rowsText;

    private PropertyTreeViewer propsEditor;
    private PropertySourceCustom propertySource;
    private TableViewer columnsTableViewer;
    private DBSAttributeBase selectedAttribute;
    private boolean firstInit = true;

    protected MockDataWizardPageSettings(MockDataSettings mockDataSettings)
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
            columnsTableViewer = new TableViewer(generatorsGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            final Table table = columnsTableViewer.getTable();

            GridData layoutData = new GridData(GridData.FILL_VERTICAL);
            layoutData.widthHint = 280;
            table.setLayoutData(layoutData);
            table.setHeaderVisible(true);
            table.setLinesVisible(true);

            columnsTableViewer.setContentProvider(new IStructuredContentProvider() {
                @Override
                public void dispose() { }
                @Override
                public void inputChanged(Viewer viewer, Object oldInput, Object newInput) { }
                @Override
                public Object[] getElements(Object inputElement) {
                    if (inputElement instanceof Collection) {
                        return ((Collection<?>) inputElement).toArray();
                    }
                    return new Object[0];
                }
            });

            CellLabelProvider labelProvider = new CellLabelProvider() {
                @Override
                public void update(ViewerCell cell) {
                    DBSAttributeBase attribute = (DBSAttributeBase) cell.getElement();
                    AttributeGeneratorProperties attributeGeneratorProperties = mockDataSettings.getAttributeGeneratorProperties(attribute);
                    if (cell.getColumnIndex() == 0) {
                        cell.setImage(DBeaverIcons.getImage(DBValueFormatting.getTypeImage(attribute)));
                        cell.setText(attribute.getName());
                        if (attributeGeneratorProperties.isEmpty()) {
                            cell.setForeground(table.getDisplay().getSystemColor(SWT.COLOR_RED));
                            noGeneratorInfoLabel.setVisible(true);
                        }
                    } else {
                        if (!attributeGeneratorProperties.isEmpty()) {
                            String selectedGenerator = attributeGeneratorProperties.getSelectedGeneratorId();
                            cell.setText(mockDataSettings.getGeneratorDescriptor(selectedGenerator).getLabel());
                        }
                    }
                }
            };

            TableViewerColumn attributeColumn = new TableViewerColumn(columnsTableViewer, SWT.LEFT);
            attributeColumn.setLabelProvider(labelProvider);
            attributeColumn.getColumn().setText("Attribute"); // header

            TableViewerColumn generatorColumn = new TableViewerColumn(columnsTableViewer, SWT.LEFT);
            generatorColumn.setLabelProvider(labelProvider);
            generatorColumn.getColumn().setText("Generator"); // header

            generatorColumn.setEditingSupport(new EditingSupport(columnsTableViewer) {
                @Override
                protected CellEditor getCellEditor(Object element) {
                    DBSAttributeBase attribute = (DBSAttributeBase) element;

                    AttributeGeneratorProperties attributeGenerators = mockDataSettings.getAttributeGeneratorProperties(attribute);
                    Set<String> generators = new LinkedHashSet<>();
                    if (attributeGenerators.isEmpty()) {
                        // TODO item.setForeground(columnsTableViewer.getDisplay().getSystemColor(SWT.COLOR_RED));
                        noGeneratorInfoLabel.setVisible(true);
                    } else {
                        for (String generatorId : attributeGenerators.getGenerators()) {
                            generators.add(mockDataSettings.getGeneratorDescriptor(generatorId).getLabel());
                        }
                    }

                    CustomComboBoxCellEditor customComboBoxCellEditor = new CustomComboBoxCellEditor(
                            columnsTableViewer,
                            columnsTableViewer.getTable(),
                            generators.toArray(new String[generators.size()]),
                            SWT.BORDER);
                    return customComboBoxCellEditor;
                }

                @Override
                protected boolean canEdit(Object element) { return true; }

                @Override
                protected Object getValue(Object element) {
                    DBSAttributeBase attribute = (DBSAttributeBase) element;
                    String selectedGenerator = mockDataSettings.getAttributeGeneratorProperties(attribute).getSelectedGeneratorId();
                    return mockDataSettings.getGeneratorDescriptor(selectedGenerator).getLabel();
                }

                @Override
                protected void setValue(Object element, Object value) {
                    DBSAttributeBase attribute = (DBSAttributeBase) element;
                    MockGeneratorDescriptor generatorForName = mockDataSettings.findGeneratorForName((String) value);
                    saveGeneratorProperties();
                    reloadProperties(attribute, generatorForName.getId());
                    columnsTableViewer.refresh(true, true);
                }
            });

            table.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    saveGeneratorProperties();
                    reloadProperties((DBSAttributeBase) e.item.getData(), null);
                }
            });

            propsEditor = new PropertyTreeViewer(generatorsGroup, SWT.BORDER);
            propsEditor.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));

            noGeneratorInfoLabel = UIUtils.createInfoLabel(composite,
                    "Generators for the red highlighted attributes aren't found. So, no data will be generated for them.");
            //noGeneratorInfoLabel.setForeground(columnsTableViewer.getDisplay().getSystemColor(SWT.COLOR_RED));
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            gd.verticalIndent = 5;
            noGeneratorInfoLabel.setLayoutData(gd);
            noGeneratorInfoLabel.setVisible(false);

            table.addControlListener(new ControlAdapter() {
                @Override
                public void controlResized(ControlEvent e)
                {
                    UIUtils.packColumns(table);
                    UIUtils.maxTableColumnsWidth(table);
                    table.removeControlListener(this);
                }
            });
        }

        setControl(composite);
    }

    @Override
    public void activatePage() {

        try {
            // init the generators properties
            if (firstInit) {
                firstInit = false;
                Collection<? extends DBSAttributeBase> attributes = this.mockDataSettings.init(getWizard());
                columnsTableViewer.setInput(attributes);
            }

            // select the first item
            final Table table = columnsTableViewer.getTable();
            table.select(0);
            // and notify the listeners
            Event event = new Event();
            event.widget = table;
            event.display = table.getDisplay();
            event.item = table.getItem(0);
            event.type = SWT.Selection;
            table.notifyListeners(SWT.Selection, event);
        } catch (DBException e) {
            e.printStackTrace();
        }

        updatePageCompletion();
    }

    @Override
    public void deactivatePage() {
        saveGeneratorProperties();
    }

    @Override
    public boolean isPageComplete() {
        return true;
    }

    private void updateState() {
        mockDataSettings.setRemoveOldData(removeOldDataCheck.getSelection());
        mockDataSettings.setRowsNumber(Long.parseLong(rowsText.getText()));
    }

    private void reloadProperties(DBSAttributeBase attribute, String generatorId) {
        AttributeGeneratorProperties attributeGeneratorProperties = mockDataSettings.getAttributeGeneratorProperties(attribute);
        if (generatorId == null) {
            generatorId = attributeGeneratorProperties.getSelectedGeneratorId();
        }
        if (attribute == selectedAttribute) {
            String selectedGenerator = attributeGeneratorProperties.getSelectedGeneratorId();
            if (Objects.equals(selectedGenerator, generatorId)) {
                // do nothing
                return;
            }
        }
        selectedAttribute = attribute;
        generatorId = attributeGeneratorProperties.setSelectedGeneratorId(generatorId);
        propertySource = attributeGeneratorProperties.getGeneratorPropertySource(generatorId);
        if (propertySource != null) {
            propsEditor.loadProperties(propertySource);
            propsEditor.setExpandMode(PropertyTreeViewer.ExpandMode.FIRST);
            propsEditor.expandAll();
        } else {
            propsEditor.clearProperties();
        }
    }

    private void saveGeneratorProperties() {
        if (selectedAttribute != null) {
            AttributeGeneratorProperties attributeGeneratorProperties = mockDataSettings.getAttributeGeneratorProperties(selectedAttribute);
            String selectedGenerator = attributeGeneratorProperties.getSelectedGeneratorId();
            attributeGeneratorProperties.putGeneratorPropertySource(selectedGenerator, propertySource);
        }
    }
}
