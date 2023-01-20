/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.gis.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.gis.GeometryViewerConstants;
import org.jkiss.dbeaver.ui.gis.internal.GISMessages;
import org.jkiss.dbeaver.ui.gis.internal.GISViewerActivator;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class PrefPageGIS extends AbstractPrefPage implements IWorkbenchPreferencePage {
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.gis";

    private Text defaultSridText;
    private Text maxObjectsText;
    private Spinner minZoomLevelSpinner;

    @Override
    public void init(IWorkbench workbench) {
        // nothing to initialize
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        final Composite composite = UIUtils.createComposite(parent, 1);
        final DBPPreferenceStore preferences = GISViewerActivator.getDefault().getPreferences();

        {
            final Group group = UIUtils.createControlGroup(composite, GISMessages.pref_page_gis_viewer_group, 2, SWT.NONE, 0);

            defaultSridText = UIUtils.createLabelText(group, GISMessages.pref_page_gis_viewer_label_srid, preferences.getString(GeometryViewerConstants.PREF_DEFAULT_SRID), SWT.BORDER);
            defaultSridText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));

            maxObjectsText = UIUtils.createLabelText(group, GISMessages.pref_page_gis_viewer_label_max_objects, preferences.getString(GeometryViewerConstants.PREF_MAX_OBJECTS_RENDER), SWT.BORDER);
            maxObjectsText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));

            minZoomLevelSpinner = UIUtils.createLabelSpinner(group, GISMessages.pref_page_gis_viewer_spinner_min_zoom_level, preferences.getInt(GeometryViewerConstants.PREF_MIN_ZOOM_LEVEL), GeometryViewerConstants.DEFAULT_MIN_ZOOM_LEVEL, GeometryViewerConstants.DEFAULT_MAX_ZOOM_LEVEL);
        }

        return composite;
    }

    @Override
    protected void performDefaults() {
        final DBPPreferenceStore preferences = GISViewerActivator.getDefault().getPreferences();
        defaultSridText.setText(preferences.getDefaultString(GeometryViewerConstants.PREF_DEFAULT_SRID));
        maxObjectsText.setText(preferences.getDefaultString(GeometryViewerConstants.PREF_MAX_OBJECTS_RENDER));
        minZoomLevelSpinner.setSelection(preferences.getDefaultInt(GeometryViewerConstants.PREF_MIN_ZOOM_LEVEL));
    }

    @Override
    public boolean performOk() {
        final DBPPreferenceStore preferences = GISViewerActivator.getDefault().getPreferences();
        preferences.setValue(GeometryViewerConstants.PREF_DEFAULT_SRID, CommonUtils.toInt(defaultSridText.getText()));
        preferences.setValue(GeometryViewerConstants.PREF_MAX_OBJECTS_RENDER, CommonUtils.toInt(maxObjectsText.getText()));
        preferences.setValue(GeometryViewerConstants.PREF_MIN_ZOOM_LEVEL, CommonUtils.toInt(minZoomLevelSpinner.getText()));

        PrefUtils.savePreferenceStore(preferences);

        return true;
    }
}
