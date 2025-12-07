/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.project;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.DataSourceNavigatorSettings;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.List;

public class PrefPageProjectNavigatorView extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

    private static final Log log = Log.getLog(PrefPageProjectNavigatorView.class);
    private static final String KEY_NAV_VIEW = "navigator.default.view";

    private static final List<DataSourceNavigatorSettings.Preset> PRESETS = List.of(
        DataSourceNavigatorSettings.PRESET_SIMPLE,
        DataSourceNavigatorSettings.PRESET_FULL,
        DataSourceNavigatorSettings.PRESET_CUSTOM
    );

    private Combo combo;
    private DBPProject projectMeta;

    public PrefPageProjectNavigatorView() {
        setTitle(UINavigatorMessages.pref_page_navigator_view_title);
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite container = UIUtils.createPlaceholder(parent, 1);

        Group group = UIUtils.createControlGroup(
            container,
            UINavigatorMessages.pref_page_navigator_view_group_settings,
            2, GridData.FILL_HORIZONTAL, 0
        );

        combo = UIUtils.createLabelCombo(
            group,
            UINavigatorMessages.pref_page_navigator_view_label_connection_view,
            SWT.DROP_DOWN | SWT.READ_ONLY
        );
        combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));


        for (DataSourceNavigatorSettings.Preset preset : PRESETS) {
            combo.add(preset.getName());
        }

        loadValues();

        if (DBWorkbench.isDistributed() && !currentUserIsAdmin()) {
            combo.setEnabled(false);
            setMessage(UINavigatorMessages.pref_page_navigator_view_message_admin_only, WARNING);
        }

        return container;
    }

    private boolean currentUserIsAdmin() {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(DBAPermissionRealm.PERMISSION_ADMIN);
    }

    private void loadValues() {
        if (projectMeta == null) {
            combo.select(0);
            return;
        }

        var dataSources = projectMeta.getDataSourceRegistry().getDataSources();
        if (dataSources.isEmpty()) {
            combo.select(0);
            return;
        }

        DBNBrowseSettings firstSettings = dataSources.getFirst().getNavigatorSettings();
        boolean allSame = true;

        for (int i = 1; i < dataSources.size(); i++) {
            DBNBrowseSettings currentSettings = dataSources.get(i).getNavigatorSettings();
            if (!firstSettings.equals(currentSettings)) {
                allSame = false;
                break;
            }
        }

        int selectedIndex = 0;

        if (!allSame) {
            selectedIndex = PRESETS.indexOf(DataSourceNavigatorSettings.PRESET_CUSTOM);
        } else {
            for (var preset : DataSourceNavigatorSettings.PRESETS.values()) {
                if (preset.getSettings().equals(firstSettings)) {
                    selectedIndex = PRESETS.indexOf(preset);
                    break;
                }
            }
        }
        combo.select(selectedIndex);
    }

    @Override
    public boolean performOk() {
        if (projectMeta == null || combo == null) {
            return super.performOk();
        }

        int selectedIndex = combo.getSelectionIndex();
        String selectedValue = PRESETS.get(selectedIndex).getId();

        try {
            projectMeta.setProjectProperty(KEY_NAV_VIEW, selectedValue);
            DBPDataSourceRegistry dataSourceRegistry = projectMeta.getDataSourceRegistry();

            var dataSources = dataSourceRegistry.getDataSources();
            for (DBPDataSourceContainer ds : dataSources) {
                if (ds instanceof DataSourceDescriptor descriptor) {
                    descriptor.setNavigatorSettings(DataSourceNavigatorSettings.getDefaultSettings(selectedValue, true));
                }
            }
            dataSourceRegistry.flushConfig();
        } catch (Exception e) {
            log.error("Error saving connection view setting", e);
            DBWorkbench.getPlatformUI().showError(
                UINavigatorMessages.pref_page_navigator_view_error_settings_title,
                UINavigatorMessages.pref_page_navigator_view_error_settings_message,
                e
            );
            return false;
        }

        return super.performOk();
    }

    @Override
    public IAdaptable getElement() {
        return projectMeta instanceof RCPProject rcpProject ? rcpProject.getEclipseProject() : null;
    }

    @Override
    public void setElement(IAdaptable element) {
        IProject project;
        if (element instanceof DBNNode node && node.getOwnerProject() instanceof RCPProject rcpProject) {
            project = rcpProject.getEclipseProject();
        } else {
            project = GeneralUtils.adapt(element, IProject.class);
        }
        if (project != null) {
            this.projectMeta = DBPPlatformDesktop.getInstance().getWorkspace().getProject(project);
        }
    }
}
