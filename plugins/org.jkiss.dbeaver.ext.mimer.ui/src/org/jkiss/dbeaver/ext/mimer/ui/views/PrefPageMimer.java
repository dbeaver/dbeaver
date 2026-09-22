/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mimer.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PreferenceStoreDelegate;
import org.jkiss.dbeaver.ui.preferences.TargetPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mimer SQL global preferences: the "Don't ask again" state for the extra CASCADE-drop
 * confirmation ({@code org.jkiss.dbeaver.ext.mimer.edit.MimerCascadeDropUtil}) - one checkbox per
 * object type, each backed by {@link MimerConstants#PREF_DROP_CASCADE_CONFIRM_PREFIX}.
 * Unchecking one (or "Restore Defaults") makes that drop prompt again. Production connections
 * always prompt regardless of these. Also four GIS display toggles (see {@code model.gis} - one
 * auto-apply switch per {@code BUILTIN.GIS_*} type, plus one shared grid-text switch) - global
 * only, no per-connection/per-column override.
 *
 * @author Mimer Information Technology
 */
public class PrefPageMimer extends TargetPrefPage {

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.mimer.general";

    private final Map<String, Button> suppressChecks = new LinkedHashMap<>();
    private Button gisLocationAutoMapCheck;
    private Button gisCoordinateAutoMapCheck;
    private Button gisLatLongAutoDecimalCheck;
    private Button gisGridPointTextCheck;

    public PrefPageMimer() {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBWorkbench.getPlatform().getPreferenceStore()));
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor) {
        return false;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions() {
        return false;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);

        Composite group = UIUtils.createTitledComposite(
            composite, "CASCADE drop confirmations", 2, GridData.FILL_HORIZONTAL);

        Label intro = new Label(group, SWT.WRAP);
        intro.setText(
            "When you drop an object with CASCADE, Mimer SQL asks for an extra confirmation because\n"
                + "CASCADE recursively drops every dependent object. Checking \"Don't ask again\" in that\n"
                + "dialog suppresses it per object type - the checkboxes below. Production connections\n"
                + "(connection type with \"Confirm SQL execution\" enabled) always ask, regardless.");
        GridData introGd = new GridData(GridData.FILL_HORIZONTAL);
        introGd.horizontalSpan = 2;
        intro.setLayoutData(introGd);

        for (Map.Entry<String, String> type : MimerConstants.DROP_CASCADE_CONFIRM_TYPES.entrySet()) {
            Button check = UIUtils.createCheckbox(
                group,
                type.getValue(),
                "Don't ask again before a CASCADE drop of a " + type.getValue().toLowerCase(),
                false,
                1);
            suppressChecks.put(type.getKey(), check);
        }

        Composite gisGroup = UIUtils.createTitledComposite(
            composite, "GIS display", 1, GridData.FILL_HORIZONTAL);

        Label gisIntro = new Label(gisGroup, SWT.WRAP);
        gisIntro.setText(
            "There's no direct JDBC support for Mimer SQL's GIS types, so values are read as real\n"
                + "numbers by asking Mimer itself to convert its own stored bytes. Global only - not per\n"
                + "connection or per column.");
        gisIntro.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        gisLocationAutoMapCheck = UIUtils.createCheckbox(
            gisGroup,
            "Always show BUILTIN.GIS_LOCATION columns on the Spatial map view",
            "When off, GIS_LOCATION behaves like a plain binary column unless you pick \"View as...\" yourself.",
            true,
            1);
        gisCoordinateAutoMapCheck = UIUtils.createCheckbox(
            gisGroup,
            "Always show BUILTIN.GIS_COORDINATE columns on the geometry viewer",
            "When off, GIS_COORDINATE behaves like a plain binary column unless you pick \"View as...\" yourself.",
            true,
            1);
        gisLatLongAutoDecimalCheck = UIUtils.createCheckbox(
            gisGroup,
            "Always show BUILTIN.GIS_LATITUDE/GIS_LONGITUDE columns as a decimal number",
            "When off, these behave like plain binary columns unless you pick \"View as...\" yourself.",
            true,
            1);
        gisGridPointTextCheck = UIUtils.createCheckbox(
            gisGroup,
            "Show GIS_LOCATION/GIS_COORDINATE values as \"POINT (...)\" text in the data grid",
            "When off, the data grid shows the raw binary value instead - independent of the other settings above.",
            true,
            1);

        return composite;
    }

    @Override
    protected void loadPreferences(@NotNull DBPPreferenceStore store) {
        for (Map.Entry<String, Button> e : suppressChecks.entrySet()) {
            e.getValue().setSelection(store.getBoolean(MimerConstants.PREF_DROP_CASCADE_CONFIRM_PREFIX + e.getKey()));
        }
        registerGisDefaults(store);
        gisLocationAutoMapCheck.setSelection(store.getBoolean(MimerConstants.PREF_GIS_LOCATION_AUTO_MAP));
        gisCoordinateAutoMapCheck.setSelection(store.getBoolean(MimerConstants.PREF_GIS_COORDINATE_AUTO_MAP));
        gisLatLongAutoDecimalCheck.setSelection(store.getBoolean(MimerConstants.PREF_GIS_LATLONG_AUTO_DECIMAL));
        gisGridPointTextCheck.setSelection(store.getBoolean(MimerConstants.PREF_GIS_GRID_POINT_TEXT));
    }

    @Override
    protected void savePreferences(@NotNull DBPPreferenceStore store) {
        for (Map.Entry<String, Button> e : suppressChecks.entrySet()) {
            store.setValue(MimerConstants.PREF_DROP_CASCADE_CONFIRM_PREFIX + e.getKey(), e.getValue().getSelection());
        }
        // Must be registered before setValue() below - otherwise an unticked (false) checkbox
        // compares against Eclipse's own unregistered-key fallback (also false), looks like "no
        // change", and is silently never written at all. See MimerGisUtils#registerGisDefaults
        // (the model-side equivalent) for the live bug this fixes.
        registerGisDefaults(store);
        store.setValue(MimerConstants.PREF_GIS_LOCATION_AUTO_MAP, gisLocationAutoMapCheck.getSelection());
        store.setValue(MimerConstants.PREF_GIS_COORDINATE_AUTO_MAP, gisCoordinateAutoMapCheck.getSelection());
        store.setValue(MimerConstants.PREF_GIS_LATLONG_AUTO_DECIMAL, gisLatLongAutoDecimalCheck.getSelection());
        store.setValue(MimerConstants.PREF_GIS_GRID_POINT_TEXT, gisGridPointTextCheck.getSelection());
        PrefUtils.savePreferenceStore(store);
    }

    private static void registerGisDefaults(@NotNull DBPPreferenceStore store) {
        // GIS_COORDINATE's map default is deliberately false, unlike the other three - see
        // MimerGisUtils#registerGisDefaults (the model-side copy of this same method) for the
        // full reasoning.
        store.setDefault(MimerConstants.PREF_GIS_LOCATION_AUTO_MAP, true);
        store.setDefault(MimerConstants.PREF_GIS_COORDINATE_AUTO_MAP, false);
        store.setDefault(MimerConstants.PREF_GIS_LATLONG_AUTO_DECIMAL, true);
        store.setDefault(MimerConstants.PREF_GIS_GRID_POINT_TEXT, true);
    }

    @Override
    protected void clearPreferences(@NotNull DBPPreferenceStore store) {
        for (String typeKey : suppressChecks.keySet()) {
            store.setToDefault(MimerConstants.PREF_DROP_CASCADE_CONFIRM_PREFIX + typeKey);
        }
        registerGisDefaults(store);
        store.setToDefault(MimerConstants.PREF_GIS_LOCATION_AUTO_MAP);
        store.setToDefault(MimerConstants.PREF_GIS_COORDINATE_AUTO_MAP);
        store.setToDefault(MimerConstants.PREF_GIS_LATLONG_AUTO_DECIMAL);
        store.setToDefault(MimerConstants.PREF_GIS_GRID_POINT_TEXT);
    }

    @Override
    protected String getPropertyPageID() {
        return PAGE_ID;
    }
}
