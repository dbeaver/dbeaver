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
package org.jkiss.dbeaver.ui.gis.panel;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.gis.GeometryViewerConstants;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.dbeaver.ui.gis.internal.GISViewerActivator;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

/**
 * GISViewerConfigurationDialog
 */
public class GISViewerConfigurationDialog extends BaseDialog {

    private static final Log log = Log.getLog(GISViewerConfigurationDialog.class);
    private Text defaultSridText;
    private Text maxObjectsText;

    public GISViewerConfigurationDialog(Shell shell) {
        super(shell, GISMessages.panel_gis_viewer_config_dialog_title_configure, null);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite dialogArea = super.createDialogArea(parent);

        DBPPreferenceStore preferences = GISViewerActivator.getDefault().getPreferences();

        Group crsGroup = UIUtils.createControlGroup(dialogArea, GISMessages.panel_gis_viewer_config_dialog_control_group_label, 2, SWT.NONE, 0);
        crsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        defaultSridText = UIUtils.createLabelText(crsGroup, GISMessages.panel_gis_viewer_config_dialog_label_text_srid, preferences.getString(GeometryViewerConstants.PREF_DEFAULT_SRID), SWT.BORDER);
        defaultSridText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));
        maxObjectsText = UIUtils.createLabelText(crsGroup, GISMessages.panel_gis_viewer_config_dialog_label_tixi_max_objects, preferences.getString(GeometryViewerConstants.PREF_MAX_OBJECTS_RENDER), SWT.BORDER);
        maxObjectsText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));

        return dialogArea;
    }

    @Override
    protected void okPressed() {
        DBPPreferenceStore preferences = GISViewerActivator.getDefault().getPreferences();
        preferences.setValue(GeometryViewerConstants.PREF_DEFAULT_SRID, defaultSridText.getText());
        preferences.setValue(GeometryViewerConstants.PREF_MAX_OBJECTS_RENDER, CommonUtils.toInt(maxObjectsText.getText()));
        super.okPressed();
    }
}