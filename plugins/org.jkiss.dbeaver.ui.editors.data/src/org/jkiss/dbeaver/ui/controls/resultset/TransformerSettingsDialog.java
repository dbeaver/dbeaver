/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVTransformSettings;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.Type;
import java.util.List;
import java.util.*;

public class TransformerSettingsDialog extends BaseDialog {
    private static final Log log = Log.getLog(TransformerSettingsDialog.class);

    private static final String PROP_FOR_TRANSFORMER = "propertiesForTransformerWithId=";
    private static final Type PROPERTIES_TYPE = new TypeToken<Map<String, Object>>(){}.getType();
    private static final Gson GSON = new Gson();

    private final ResultSetViewer viewer;
    private final DBVEntity vEntitySrc;
    private final DBVEntity vEntity;

    private DBDAttributeBinding currentAttribute;

    private PropertyTreeViewer propertiesEditor;
    private PropertySourceCustom propertySource;

    private boolean selector;
    private List<? extends DBDAttributeTransformerDescriptor> transformerList;
    private Text infoText;
    private DBDAttributeTransformerDescriptor transformer;
    private Combo transformerCombo;
    private Table attributeTable;

    public TransformerSettingsDialog(ResultSetViewer viewer, DBDAttributeBinding currentAttribute, boolean selector) {
        super(viewer.getControl().getShell(), DBUtils.getObjectFullName(viewer.getDataContainer(), DBPEvaluationContext.UI) + " transforms", null);
        this.viewer = viewer;
        this.currentAttribute = currentAttribute;
        this.selector = selector;

        this.vEntitySrc = this.currentAttribute == null ?
            viewer.getModel().getVirtualEntity(true) :
            DBVUtils.getVirtualEntity(currentAttribute, true);
        this.vEntity = new DBVEntity(vEntitySrc.getContainer(), vEntitySrc, vEntitySrc.getModel());
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        Composite panel = composite;
        if (selector) {
            SashForm divider = new SashForm(composite, SWT.HORIZONTAL);
            divider.setSashWidth(10);
            divider.setLayoutData(new GridData(GridData.FILL_BOTH));
            panel = divider;

            createAttributeSelectorArea(panel);
        } else {
            if (currentAttribute != null) {
                detectTransformers();
            }
        }
        createTransformSettingsArea(panel);

        if (currentAttribute != null) {
            updateTransformerInfo();
        }

        return parent;
    }

    private void createAttributeSelectorArea(Composite composite) {
        Composite panel = UIUtils.createComposite(composite, 1);

        attributeTable = new Table(panel, SWT.FULL_SELECTION | SWT.BORDER);
        attributeTable.setHeaderVisible(true);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 400;
        attributeTable.setLayoutData(gd);
        UIUtils.executeOnResize(attributeTable, () -> UIUtils.packColumns(attributeTable, true));

        UIUtils.createTableColumn(attributeTable, SWT.LEFT, "Name");
        UIUtils.createTableColumn(attributeTable, SWT.LEFT, "Transforms");

        for (DBDAttributeBinding attr : viewer.getModel().getVisibleAttributes()) {
            TableItem attrItem = new TableItem(attributeTable, SWT.NONE);;
            attrItem.setData(attr);
            attrItem.setText(0, attr.getName());
            attrItem.setImage(0, DBeaverIcons.getImage(DBValueFormatting.getObjectImage(attr, true)));
            updateTransformItem(attrItem);

            if (this.currentAttribute == attr) {
                attributeTable.setSelection(attrItem);
            }
        }

        attributeTable.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateAttributeSelection();
            }
        });
    }

    private void updateTransformItem(TableItem attrItem) {
        DBDAttributeBinding attr = (DBDAttributeBinding) attrItem.getData();
        String transformStr = "";
        DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(attr, false);
        if (vAttr != null) {
            DBVTransformSettings settings = vAttr.getTransformSettings();
            if (settings != null) {
                if (!CommonUtils.isEmpty(settings.getIncludedTransformers())) {
                    transformStr = String.join(",", settings.getIncludedTransformers());
                } else if (!CommonUtils.isEmpty(settings.getCustomTransformer())) {
                    DBDAttributeTransformerDescriptor td =
                        DBWorkbench.getPlatform().getValueHandlerRegistry().getTransformer(settings.getCustomTransformer());
                    if (td != null) {
                        transformStr = td.getName();
                    }
                }
            }
        }
        attrItem.setText(1, transformStr);
    }

    private void updateAttributeSelection() {
        if (currentAttribute != null) {
            updateAttributeItemText();
        }

        if (attributeTable.getSelectionIndex() < 0) {
            currentAttribute = null;
        } else {
            currentAttribute = (DBDAttributeBinding) attributeTable.getItem(attributeTable.getSelectionIndex()).getData();

            detectTransformers();
            updateTransformerInfo();
        }
    }

    private void detectTransformers() {
        final DBPDataSource dataSource = viewer.getDataSource();

        DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(currentAttribute, false);
        DBVTransformSettings settings = vAttr == null ? null : DBVUtils.getTransformSettings(vAttr, false);

        if (dataSource != null && settings != null && !CommonUtils.isEmpty(settings.getCustomTransformer())) {
            transformer = DBWorkbench.getPlatform().getValueHandlerRegistry().getTransformer(settings.getCustomTransformer());
        } else {
            transformer = null;
        }

        transformerList = DBWorkbench.getPlatform().getValueHandlerRegistry().findTransformers(currentAttribute.getDataSource(), currentAttribute, null);
    }

    private void updateTransformerInfo() {
        if (selector) {
            transformerCombo.removeAll();
            transformerCombo.add(ResultSetViewer.EMPTY_TRANSFORMER_NAME);
            if (transformerList != null && selector) {
                for (DBDAttributeTransformerDescriptor td : transformerList) {
                    transformerCombo.add(td.getName());
                    if (td == transformer) {
                        transformerCombo.select(transformerCombo.getItemCount() - 1);
                    }
                }
            }
            if (transformerCombo.getSelectionIndex() < 0) {
                transformerCombo.select(0);
            }
        }
        if (infoText != null) {
            if (transformer != null && transformer.getDescription() != null) {
                infoText.setText(transformer.getDescription());
            } else {
                infoText.setText("");
            }
        }

        if (transformer != null) {
            Collection<? extends DBPPropertyDescriptor> transformerProperties = transformer.getProperties();
            loadTransformerSettings(transformerProperties);
        } else {
            loadTransformerSettings(Collections.emptyList());
        }
    }

    private void saveTransformerSettings() {
        propertiesEditor.saveEditorValues();
        if (currentAttribute == null) {
            // Nothing to save - just ignore
            return;
        }
        DBVEntityAttribute vAttr = vEntity.getVirtualAttribute(currentAttribute, true);
        if (vAttr == null) {
            log.error("Can't get attribute settings for " + currentAttribute.getName());
            return;
        }
        DBVTransformSettings settings = DBVUtils.getTransformSettings(vAttr, true);
        if (settings == null) {
            log.error("Can't get transform settings for " + currentAttribute.getName());
            return;
        }
        if (selector) {
            settings.setCustomTransformer(transformer == null ? null : transformer.getId());
        }
        if (transformer == null) {
            settings.setTransformOptions(new LinkedHashMap<>());
        } else {
            final Map<String, Object> properties = propertySource.getPropertiesWithDefaults();
            for (Map.Entry<String, Object> prop : properties.entrySet()) {
                if (prop.getValue() != null) {
                    settings.setTransformOption(prop.getKey().toString(), prop.getValue().toString());
                }
            }
            getDialogSettings().put(PROP_FOR_TRANSFORMER + transformer.getId(), GSON.toJson(properties, PROPERTIES_TYPE));
        }
    }

    private void createTransformSettingsArea(Composite composite) {
        Composite settingsPanel = UIUtils.createComposite(composite, 1);
        if (composite.getLayout() instanceof GridLayout) {
            settingsPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        }
        if (selector || transformer != null) {
            final Composite placeholder = UIUtils.createControlGroup(settingsPanel, "Transformer", 2, GridData.FILL_HORIZONTAL, -1);
            if (!selector) {
                UIUtils.createLabelText(placeholder, "Name", transformer.getName(), SWT.READ_ONLY);
            } else {
                transformerCombo = UIUtils.createLabelCombo(placeholder, "Name", SWT.DROP_DOWN | SWT.READ_ONLY);
                transformerCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                transformerCombo.add(ResultSetViewer.EMPTY_TRANSFORMER_NAME);
                transformerCombo.select(0);
                transformerCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        int selectionIndex = transformerCombo.getSelectionIndex();
                        if (selectionIndex == 0) {
                            transformer = null;
                            infoText.setText("N/A");
                            loadTransformerSettings(Collections.emptyList());
                        } else {
                            transformer = transformerList.get(selectionIndex - 1);
                            infoText.setText(CommonUtils.notEmpty(transformer.getDescription()));
                            loadTransformerSettings(transformer.getProperties());
                        }
                        updateTransformerInfo();
                        updateAttributeItemText();

                        composite.layout(true, true);
                    }
                });
            }
            Label infoLabel = UIUtils.createControlLabel(settingsPanel, "Info");
            infoLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            infoText = new Text(settingsPanel, SWT.READ_ONLY | SWT.WRAP);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 300;
            infoText.setLayoutData(gd);
        }

        propertiesEditor = new PropertyTreeViewer(settingsPanel, SWT.BORDER);

        propertiesEditor.getControl().setFocus();
    }

    private void updateAttributeItemText() {
        saveTransformerSettings();
        for (TableItem item : attributeTable.getItems()) {
            if (item.getData() == currentAttribute) {
                updateTransformItem(item);
                break;
            }
        }
    }

    private void loadTransformerSettings(Collection<? extends DBPPropertyDescriptor> properties) {
        DBVTransformSettings settings = currentAttribute == null ? null : DBVUtils.getTransformSettings(currentAttribute, false);
        Map<String, Object> transformOptions = settings == null ? null : settings.getTransformOptions();
        if (transformOptions == null && transformer != null) {
            String propertiesJson = getDialogSettings().get(PROP_FOR_TRANSFORMER + transformer.getId());
            if (propertiesJson != null) {
                transformOptions = GSON.fromJson(propertiesJson, PROPERTIES_TYPE);
            }
        }
        if (transformOptions == null) {
            transformOptions = Collections.emptyMap();
        }
        propertySource = new PropertySourceCustom(
            properties,
            transformOptions);
        propertiesEditor.loadProperties(propertySource);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    public void create() {
        super.create();

        if (propertySource != null && propertySource.getProperties().length == 0 && !selector) {
            // No properties
            UIUtils.asyncExec(this::okPressed);
        }
    }

    @Override
    protected void okPressed()
    {
        saveTransformerSettings();

        vEntitySrc.copyFrom(vEntity, vEntity.getModel());
        vEntitySrc.persistConfiguration();

        super.okPressed();
    }

    @Override
    public boolean close() {
        if (this.vEntity != null) {
            this.vEntity.dispose();
        }
        return super.close();
    }

    @NotNull
    private static IDialogSettings getDialogSettings() {
        return UIUtils.getDialogSettings(TransformerSettingsDialog.class.getSimpleName());
    }
}
