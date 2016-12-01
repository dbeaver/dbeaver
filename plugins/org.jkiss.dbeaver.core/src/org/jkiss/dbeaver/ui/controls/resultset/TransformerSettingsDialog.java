/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeTransformerDescriptor;
import org.jkiss.dbeaver.model.virtual.DBVTransformSettings;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

class TransformerSettingsDialog extends BaseDialog {

    private static final Log log = Log.getLog(TransformerSettingsDialog.class);

    private final ResultSetViewer viewer;
    private final DBDAttributeBinding attr;
    private final DBVTransformSettings settings;

    private PropertyTreeViewer propertiesEditor;
    private PropertySourceCustom propertySource;

    public TransformerSettingsDialog(ResultSetViewer viewer, DBDAttributeBinding attr, DBVTransformSettings settings) {
        super(viewer.getControl().getShell(), "Transformer settings", null);
        this.viewer = viewer;
        this.attr = attr;
        this.settings = settings;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);

        final DBPDataSource dataSource = viewer.getDataContainer() == null ? null : viewer.getDataContainer().getDataSource();

        Collection<? extends DBPPropertyDescriptor> properties = Collections.emptyList();
        final DBDAttributeTransformerDescriptor transformer;
        if (dataSource != null && !CommonUtils.isEmpty(settings.getCustomTransformer())) {
            transformer = dataSource.getContainer().getPlatform().getValueHandlerRegistry().getTransformer(settings.getCustomTransformer());
            if (transformer != null) {
                properties = transformer.getProperties();
            }
        } else {
            transformer = null;
        }
        if (transformer != null) {
            final Composite placeholder = UIUtils.createControlGroup(composite, "Transformer", 2, GridData.FILL_HORIZONTAL, -1);
            UIUtils.createLabelText(placeholder, "Name", transformer.getName(), SWT.READ_ONLY);
            Label infoLabel = UIUtils.createControlLabel(placeholder, "Info");
            infoLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            final Text infoText = new Text(placeholder, SWT.READ_ONLY | SWT.WRAP);
            if (transformer.getDescription() != null) {
                infoText.setText(transformer.getDescription());
            }
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 300;
            infoText.setLayoutData(gd);
        }

        Map<String, String> transformOptions = settings.getTransformOptions();
        if (transformOptions == null) {
            transformOptions = Collections.emptyMap();
        }

        propertiesEditor = new PropertyTreeViewer(composite, SWT.BORDER);
        propertySource = new PropertySourceCustom(
            properties,
            transformOptions);
        propertiesEditor.loadProperties(propertySource);

        propertiesEditor.getControl().setFocus();

        return parent;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected void okPressed()
    {
        final Map<Object, Object> properties = propertySource.getPropertiesWithDefaults();
        for (Map.Entry<Object, Object> prop : properties.entrySet()) {
            if (prop.getValue() != null) {
                settings.setTransformOption(prop.getKey().toString(), prop.getValue().toString());
            }
        }
        super.okPressed();
    }

}
