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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mockdata.MockDataSettings.AttributeGeneratorProperties;
import org.jkiss.dbeaver.ext.mockdata.model.MockGeneratorDescriptor;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;
import java.util.List;

public class MockDataWizardPageSettings extends ActiveWizardPage<MockDataExecuteWizard>
{
    private static final Log log = Log.getLog(MockDataWizardPageSettings.class);
    public static final int DEFAULT_NAME_COLUMN_WIDTH = 110;

    private MockDataSettings mockDataSettings;

    private CLabel noGeneratorInfoLabel;
    private Text entityNameText;
    private Button removeOldDataCheck;
    private Text rowsText;
    private Text batchSizeText;

    private PropertyTreeViewer propsEditor;
    private PropertySourceCustom propertySource;
    private TableViewer generatorsTableViewer;
    private DBSAttributeBase selectedAttribute;
    private boolean firstInit = true;
    private Combo generatorCombo;
    private Label generatorDescriptionLabel;
    private Link generatorDescriptionLink;
    private Font boldFont;

    private String generatorLinkUrl;
    private transient boolean loadingSettings;

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

            Composite entityPlaceholder = UIUtils.createPlaceholder(composite, 2);
            this.entityNameText = UIUtils.createLabelText(entityPlaceholder, MockDataMessages.tools_mockdata_wizard_page_settings_text_entity, "", SWT.NONE | SWT.READ_ONLY);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            gd.widthHint = 230;
            gd.verticalIndent = 5;
            gd.horizontalIndent = 9;
            entityPlaceholder.setLayoutData(gd);

            Group settingsGroup = UIUtils.createControlGroup(
                    composite, MockDataMessages.tools_mockdata_wizard_page_settings_group_settings, 4, GridData.FILL_HORIZONTAL, 0);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.verticalIndent = 5;
            settingsGroup.setLayoutData(gd);

            this.removeOldDataCheck = UIUtils.createCheckbox(
                    settingsGroup,
                    MockDataMessages.tools_mockdata_wizard_page_settings_checkbox_remove_old_data,
                    null,
                    mockDataSettings.isRemoveOldData(), 4);
            this.removeOldDataCheck.addSelectionListener(changeListener);

            this.rowsText = UIUtils.createLabelText(
                    settingsGroup, MockDataMessages.tools_mockdata_wizard_page_settings_combo_rows, String.valueOf(mockDataSettings.getRowsNumber()), SWT.BORDER,
                    new GridData(110, SWT.DEFAULT));
            this.rowsText.addSelectionListener(changeListener);
            this.rowsText.addVerifyListener(UIUtils.getLongVerifyListener(rowsText));
            this.rowsText.addModifyListener(e -> updateState());

            this.batchSizeText = UIUtils.createLabelText(
                settingsGroup, MockDataMessages.tools_mockdata_wizard_page_settings_batch_size, String.valueOf(mockDataSettings.getBatchSize()), SWT.BORDER,
                new GridData(110, SWT.DEFAULT));
            this.batchSizeText.addSelectionListener(changeListener);
            this.batchSizeText.addVerifyListener(UIUtils.getLongVerifyListener(batchSizeText));
            this.batchSizeText.addModifyListener(e -> updateState());
        }

        {
            Group generatorsGroup = UIUtils.createControlGroup(composite, MockDataMessages.tools_mockdata_wizard_page_settings_group_generators, 5, GridData.FILL_BOTH, 0);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.verticalIndent = 5;
            generatorsGroup.setLayoutData(gd);

            Composite placeholder = UIUtils.createPlaceholder(generatorsGroup, 1);
            gd = new GridData(GridData.FILL_VERTICAL);
            gd.widthHint = 250;
            placeholder.setLayoutData(gd);

            Button autoAssignButton = new Button(placeholder, SWT.PUSH);
            autoAssignButton.setText(MockDataMessages.tools_mockdata_wizard_page_settings_button_autoassign);
            autoAssignButton.setImage(DBeaverIcons.getImage(UIIcon.OBJ_REFRESH));
            autoAssignButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (UIUtils.confirmAction(getShell(), MockDataMessages.tools_mockdata_wizard_title, MockDataMessages.tools_mockdata_wizard_page_settings_button_autoassign_confirm)) {
                        autoAssignGenerators();
                    }
                }
            });
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
//            gd.horizontalIndent = 5;
            autoAssignButton.setLayoutData(gd);

            generatorsTableViewer = new TableViewer(placeholder, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            final Table table = generatorsTableViewer.getTable();

            gd = new GridData(GridData.FILL_BOTH);
            gd.verticalIndent = 5;
            table.setLayoutData(gd);
            table.setHeaderVisible(true);
            table.setLinesVisible(true);

            generatorsTableViewer.setContentProvider(new IStructuredContentProvider() {
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
                        try {
                            if (DBUtils.checkUnique(mockDataSettings.getMonitor(), mockDataSettings.getEntity(), attribute) != null) {
                                cell.setFont(boldFont);
                            }
                        } catch (DBException e) {
                            log.error("Error checking the attribute '" + attribute.getName() + "' properties", e);
                        }
                        if (attributeGeneratorProperties != null && attributeGeneratorProperties.isEmpty()) {
                            cell.setForeground(table.getDisplay().getSystemColor(SWT.COLOR_RED));
                            noGeneratorInfoLabel.setVisible(true);
                        }
                    } else {
                        if (attributeGeneratorProperties != null && !attributeGeneratorProperties.isEmpty()) {
                            String selectedGeneratorId = attributeGeneratorProperties.getSelectedGeneratorId();
                            String label = mockDataSettings.getGeneratorDescriptor(selectedGeneratorId).getLabel();
                            cell.setText(label.trim());
                        }
                    }
                }
            };

            TableViewerColumn attributeColumn = new TableViewerColumn(generatorsTableViewer, SWT.LEFT);
            attributeColumn.setLabelProvider(labelProvider);
            attributeColumn.getColumn().setText(MockDataMessages.tools_mockdata_wizard_page_settings_generatorselector_attribute);

            TableViewerColumn generatorColumn = new TableViewerColumn(generatorsTableViewer, SWT.LEFT);
            generatorColumn.setLabelProvider(labelProvider);
            TableColumn column = generatorColumn.getColumn();
            column.setText(MockDataMessages.tools_mockdata_wizard_page_settings_generatorselector_generator);

            generatorColumn.setEditingSupport(new EditingSupport(generatorsTableViewer) {
                @Override
                protected CellEditor getCellEditor(Object element) {
                    DBSAttributeBase attribute = (DBSAttributeBase) element;

                    AttributeGeneratorProperties attributeGenerators = mockDataSettings.getAttributeGeneratorProperties(attribute);
                    Set<String> generators = new LinkedHashSet<>();
                    if (attributeGenerators.isEmpty()) {
                        noGeneratorInfoLabel.setVisible(true);
                        TextCellEditor textCellEditor = new TextCellEditor(generatorsTableViewer.getTable());
                        textCellEditor.getControl().setEnabled(false);
                        return textCellEditor;
                    } else {
                        for (String generatorId : attributeGenerators.getGenerators()) {
                            if (!CommonUtils.isEmpty(generatorId)) {
                                MockGeneratorDescriptor generatorDescriptor = mockDataSettings.getGeneratorDescriptor(generatorId);
                                if (generatorDescriptor != null) {
                                    generators.add(generatorDescriptor.getLabel());
                                }
                            }
                        }

                        CustomComboBoxCellEditor customComboBoxCellEditor = new CustomComboBoxCellEditor(
                                generatorsTableViewer,
                                generatorsTableViewer.getTable(),
                                generators.toArray(new String[generators.size()]),
                                SWT.BORDER | SWT.READ_ONLY);
                        return customComboBoxCellEditor;
                    }
                }

                @Override
                protected boolean canEdit(Object element) { return true; } // disable the generator selection

                @Override
                protected Object getValue(Object element) {
                    DBSAttributeBase attribute = (DBSAttributeBase) element;
                    String selectedGenerator = mockDataSettings.getAttributeGeneratorProperties(attribute).getSelectedGeneratorId();
                    if (!CommonUtils.isEmpty(selectedGenerator)) {
                        return mockDataSettings.getGeneratorDescriptor(selectedGenerator).getLabel();
                    } else {
                        return "";
                    }
                }

                @Override
                protected void setValue(Object element, Object value) {
                    DBSAttributeBase attribute = (DBSAttributeBase) element;
                    selectGenerator(attribute, (String) value);
                }
            });

            table.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    saveGeneratorProperties();
                    reloadProperties((DBSAttributeBase) e.item.getData(), null);
                }
            });

            // generator properties
            placeholder = UIUtils.createPlaceholder(generatorsGroup, 1);
            placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));

            Composite labelCombo = UIUtils.createPlaceholder(placeholder, 5);
            gd = new GridData(GridData.FILL_HORIZONTAL);
            labelCombo.setLayoutData(gd);

            generatorCombo = new Combo(labelCombo, SWT.READ_ONLY | SWT.DROP_DOWN);
            gd = new GridData();
            gd.widthHint = 80;
            generatorCombo.setLayoutData(gd);
            generatorCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    selectGenerator(selectedAttribute, generatorCombo.getText());
                }
            });

            Composite descriptionCombo = UIUtils.createPlaceholder(labelCombo, 2);
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
            descriptionCombo.setLayoutData(gd);

            generatorDescriptionLabel = new Label(descriptionCombo, SWT.NONE);
            generatorDescriptionLabel.setFont(
                    JFaceResources.getFontRegistry().getItalic(JFaceResources.DEFAULT_FONT)
            );
            generatorDescriptionLabel.setText("");
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
            gd.horizontalIndent = 5;
            generatorDescriptionLabel.setLayoutData(gd);

            generatorDescriptionLink = UIUtils.createLink(descriptionCombo, "", new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (!CommonUtils.isEmpty(generatorLinkUrl)) {
                        IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
                        try {
                            support.getExternalBrowser().openURL(new URL(generatorLinkUrl));
                        } catch (Exception ex) {}
                    }
                }
            });
            gd = new GridData(GridData.HORIZONTAL_ALIGN_END);
            gd.horizontalIndent = 5;
            generatorDescriptionLink.setLayoutData(gd);

            Button resetButton = new Button(labelCombo, SWT.PUSH);
            resetButton.setText(MockDataMessages.tools_mockdata_wizard_page_settings_button_reset);
            resetButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    for (Object key : propertySource.getProperties().keySet()) {
                        propertySource.resetPropertyValueToDefault(key);
                    }
                    propsEditor.loadProperties(propertySource);
                    generatorsTableViewer.refresh(true, true);
                }
            });
            gd = new GridData();
            gd.horizontalIndent = 5;
            resetButton.setLayoutData(gd);

            propsEditor = new PropertyTreeViewer(placeholder, SWT.BORDER);
            gd = new GridData(GridData.FILL_BOTH);
            gd.verticalIndent = 5;
            propsEditor.getControl().setLayoutData(gd);

            noGeneratorInfoLabel = UIUtils.createInfoLabel(composite,
                    MockDataMessages.tools_mockdata_wizard_page_settings_button_info_notfound);
            //noGeneratorInfoLabel.setForeground(generatorsTableViewer.getDisplay().getSystemColor(SWT.COLOR_RED));
            gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
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

        boldFont = UIUtils.makeBoldFont(generatorsTableViewer.getControl().getFont());
    }

    private void autoAssignGenerators() {
        Map<String, AttributeGeneratorProperties> attributeGenerators = mockDataSettings.getAttributeGenerators();
        for (String attrName : attributeGenerators.keySet()) {
            mockDataSettings.autoAssignGenerator(attributeGenerators.get(attrName));
        }
        generatorsTableViewer.refresh(true, true);
    }

    private void selectGenerator(DBSAttributeBase attribute, String generatorName) {
        if (CommonUtils.isEmpty(generatorName)) {
            return;
        }
        MockGeneratorDescriptor generatorForName = mockDataSettings.findGeneratorForName(attribute, generatorName);
        if (generatorForName != null) {
            saveGeneratorProperties();
            reloadProperties(attribute, generatorForName.getId());
        }
        generatorsTableViewer.refresh(true, true);
    }

    @Override
    public void activatePage() {

        // init the generators properties
        if (firstInit) {
            try {
                UIUtils.run(this.getContainer(), true, true, monitor -> {
                    try {
                        firstInit = false;
                        MockDataExecuteWizard wizard = getWizard();
                        mockDataSettings.init(monitor, wizard);
                        wizard.loadSettings();
                    } catch (DBException ex) {
                        throw new InvocationTargetException(ex);
                    }
                });
            } catch (InvocationTargetException ex) {
                log.error("Error Mock Data Settings initialization", ex.getTargetException());
            }
            catch (InterruptedException e) {
                log.error("Mock Data Settings initialization interrupted", e);
            }

            loadingSettings = true;
            try {
                removeOldDataCheck.setSelection(mockDataSettings.isRemoveOldData());
                rowsText.setText(String.valueOf(mockDataSettings.getRowsNumber()));
                batchSizeText.setText(String.valueOf(mockDataSettings.getBatchSize()));
                generatorsTableViewer.setInput(mockDataSettings.getAttributes());
            } finally {
                loadingSettings = false;
            }
        }

        entityNameText.setText(DBUtils.getObjectFullName(mockDataSettings.getEntity(), DBPEvaluationContext.DML));
        propsEditor.getControl().setFocus();

        // select the attributes table item
        final Table table = generatorsTableViewer.getTable();
        if (table.getItemCount() > 0) {
            int selectedItemIndex = 0;
            TableItem selectedItem = null;
            String selectedAttribute = mockDataSettings.getSelectedAttribute();
            if (selectedAttribute != null) {
                for (int i = 0; i < table.getItemCount(); i++) {
                    if (selectedAttribute.equals(table.getItem(i).getText())) {
                        selectedItemIndex = i;
                        selectedItem = table.getItem(i);
                        break;
                    }
                }
            }
            table.select(selectedItemIndex);
            if (selectedItem != null) {
                table.showItem(selectedItem);
            }
            // and notify the listeners
            Event event = new Event();
            event.widget = table;
            event.display = table.getDisplay();
            event.item = table.getItem(selectedItemIndex);
            event.type = SWT.Selection;
            table.notifyListeners(SWT.Selection, event);
        } else {
            noGeneratorInfoLabel.setText(MockDataMessages.tools_mockdata_wizard_page_settings_button_info_noattributes);
            noGeneratorInfoLabel.setVisible(true);
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

    public boolean validateProperties() {
        Map<String, AttributeGeneratorProperties> attributeGenerators = mockDataSettings.getAttributeGenerators();
        for (String attr : attributeGenerators.keySet()) {
            AttributeGeneratorProperties attributeGeneratorProperties = attributeGenerators.get(attr);
            String selectedGeneratorId = attributeGeneratorProperties.getSelectedGeneratorId();
            if (!CommonUtils.isEmpty(selectedGeneratorId)) {
                Map<Object, Object> properties =
                        attributeGeneratorProperties.getGeneratorPropertySource(selectedGeneratorId).getPropertiesWithDefaults();
                for (Object key : properties.keySet()) {
                    Object value = properties.get(key);
                    // all the numeric properties shouldn't be negative
                    if (value instanceof Integer && (((Integer) value) < 0)) {
                        return false;
                    }
                    if (value instanceof Long && (((Long) value) < 0)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void updateState() {
        if (!loadingSettings) {
            mockDataSettings.setRemoveOldData(removeOldDataCheck.getSelection());
            mockDataSettings.setRowsNumber(CommonUtils.toLong(rowsText.getText()));
            mockDataSettings.setBatchSize(CommonUtils.toInt(batchSizeText.getText()));
        }
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
        mockDataSettings.setSelectedAttribute(attribute.getName());
        generatorId = attributeGeneratorProperties.setSelectedGeneratorId(generatorId);

        // set properties
        propertySource = attributeGeneratorProperties.getGeneratorPropertySource(generatorId);
        if (propertySource != null) {
            propsEditor.loadProperties(propertySource);
            propsEditor.setExpandMode(PropertyTreeViewer.ExpandMode.FIRST);
            propsEditor.expandAll();
        } else {
            propsEditor.clearProperties();
        }

        // set the properties table columns width
        UIUtils.asyncExec(() -> {
            ((Tree) propsEditor.getControl()).getColumn(0).setWidth(DEFAULT_NAME_COLUMN_WIDTH);
            ((Tree) propsEditor.getControl()).getColumn(1).setWidth(
                    propsEditor.getControl().getSize().x - DEFAULT_NAME_COLUMN_WIDTH - 30);
        });

        // generator combo & description
        List<String> generators = new ArrayList<>();
        for (String genId : attributeGeneratorProperties.getGenerators()) {
            MockGeneratorDescriptor generatorDescriptor = mockDataSettings.getGeneratorDescriptor(genId);
            if (generatorDescriptor != null) {
                generators.add(generatorDescriptor.getLabel());
            }
        }
        generatorDescriptionLink.setVisible(false);
        if (!generators.isEmpty()) {
            generatorCombo.setItems(generators.toArray(new String[generators.size()]));
            MockGeneratorDescriptor generatorDescriptor = mockDataSettings.getGeneratorDescriptor(generatorId);
            generatorCombo.setText(generatorDescriptor.getLabel());
            generatorCombo.setEnabled(true);
            generatorDescriptionLabel.setText(generatorDescriptor.getDescription());
            if (!CommonUtils.isEmpty(generatorDescriptor.getLink())) {
                generatorDescriptionLink.setText("<a>" + generatorDescriptor.getLink() + "</a>");
                generatorLinkUrl = generatorDescriptor.getUrl();
                generatorDescriptionLink.setVisible(true);
            }
        } else {
            generatorCombo.setItems(new String[] {MockDataMessages.tools_mockdata_wizard_page_settings_notfound});
            generatorCombo.setText(MockDataMessages.tools_mockdata_wizard_page_settings_notfound);
            generatorCombo.setEnabled(false);
            generatorDescriptionLabel.setText("");
        }
        generatorDescriptionLink.getParent().layout();
    }

    private void saveGeneratorProperties() {
        if (selectedAttribute != null) {
            AttributeGeneratorProperties attributeGeneratorProperties = mockDataSettings.getAttributeGeneratorProperties(selectedAttribute);
            String selectedGenerator = attributeGeneratorProperties.getSelectedGeneratorId();
            if (selectedGenerator != null) {
                attributeGeneratorProperties.putGeneratorPropertySource(selectedGenerator, propertySource);
            }
        }
    }
}
