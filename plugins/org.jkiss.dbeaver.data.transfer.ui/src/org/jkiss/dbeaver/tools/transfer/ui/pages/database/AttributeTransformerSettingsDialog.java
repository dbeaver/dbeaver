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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingAttribute;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferAttributeTransformerDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class AttributeTransformerSettingsDialog extends BaseDialog {

    private static final Log log = Log.getLog(AttributeTransformerSettingsDialog.class);

    private final DatabaseMappingAttribute mapping;
    private final DataTransferAttributeTransformerDescriptor transformer;

    private PropertyTreeViewer propertiesEditor;
    private PropertySourceCustom propertySource;

    private Text infoText;

    public AttributeTransformerSettingsDialog(Shell parentShell, DatabaseMappingAttribute mapping, DataTransferAttributeTransformerDescriptor transformer) {
        super(parentShell, "Transformer " + transformer.getName() + " settings", null);
        this.mapping = mapping;
        this.transformer = transformer;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        createTransformSettingsArea(composite);

        updateTransformerInfo();

        return parent;
    }

    private void updateTransformerInfo() {
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
        Map<String, Object> settings = mapping.getTransformerProperties();
        if (settings == null) {
            settings = new LinkedHashMap<>();
        }
        final Map<String, Object> properties = propertySource.getPropertiesWithDefaults();
        for (Map.Entry<String, Object> prop : properties.entrySet()) {
            if (prop.getValue() != null) {
                settings.put(prop.getKey(), prop.getValue());
            }
        }
        mapping.setTransformerProperties(settings);
    }

    private void createTransformSettingsArea(Composite composite) {
        Composite settingsPanel = UIUtils.createComposite(composite, 1);
        if (composite.getLayout() instanceof GridLayout) {
            settingsPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
        }
        final Composite placeholder = UIUtils.createComposite(settingsPanel, 2);
        UIUtils.createLabelText(placeholder, "Transformer", transformer.getName(), SWT.READ_ONLY);
        Label infoLabel = UIUtils.createControlLabel(placeholder, "Info");
        infoLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        infoText = new Text(placeholder, SWT.READ_ONLY | SWT.WRAP);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 300;
        infoText.setLayoutData(gd);

        propertiesEditor = new PropertyTreeViewer(settingsPanel, SWT.BORDER);

        propertiesEditor.getControl().setFocus();
    }

    private void loadTransformerSettings(Collection<? extends DBPPropertyDescriptor> properties) {
        Map<String, Object> transformOptions = mapping.getTransformerProperties();
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

        if (propertySource != null && propertySource.getProperties().length == 0) {
            // No properties
            UIUtils.asyncExec(this::okPressed);
        }
    }

    @Override
    protected void okPressed()
    {
        saveTransformerSettings();

        super.okPressed();
    }

    @Override
    public boolean close() {
        return super.close();
    }
}
