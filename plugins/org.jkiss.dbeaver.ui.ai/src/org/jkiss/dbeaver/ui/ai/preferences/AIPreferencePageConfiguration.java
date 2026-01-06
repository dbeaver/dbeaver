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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AISchemaGenerator;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.impl.AISchemaGeneratorImpl;
import org.jkiss.dbeaver.model.ai.registry.AIAssistantRegistry;
import org.jkiss.dbeaver.model.ai.registry.AISettingsManager;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.ai.format.DefaultFormattingConfigurator;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.io.IOException;

public class AIPreferencePageConfiguration extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    private static final Log log = Log.getLog(AIPreferencePageConfiguration.class);
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai.config";
    private final AISettings settings;
    private AISchemaGenerator ddlGenerator;

    private IObjectPropertyConfigurator<AISchemaGenerator, AISettings> formatterConfigurator;

    public AIPreferencePageConfiguration() {
        this.settings = AISettingsManager.getInstance().getSettings();
        try {
            ddlGenerator = AIAssistantRegistry.getInstance().getDescriptor().createSchemaGenerator();
        } catch (DBException e) {
            log.error("Formatter not found", e);
            ddlGenerator = new AISchemaGeneratorImpl();
        }
        UIPropertyConfiguratorDescriptor cfgDescriptor =
            UIPropertyConfiguratorRegistry.getInstance().getDescriptor(ddlGenerator.getClass().getName());
        if (cfgDescriptor != null) {
            try {
                formatterConfigurator = cfgDescriptor.createConfigurator();
            } catch (DBException e) {
                log.error(e);
            }
        }
        if (formatterConfigurator == null) {
            formatterConfigurator = new DefaultFormattingConfigurator();
        }
    }

    @Override
    public IAdaptable getElement() {
        return settings;
    }

    @Override
    public void setElement(IAdaptable element) {

    }

    @Override
    protected void performDefaults() {
        if (!hasAccessToPage()) {
            return;
        }
        formatterConfigurator.loadSettings(this.settings);
    }

    @Override
    public boolean performOk() {
        if (!hasAccessToPage()) {
            return false;
        }
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        formatterConfigurator.saveSettings(this.settings);
        AISettingsManager.getInstance().saveSettings(this.settings);
        try {
            store.save();
        } catch (IOException e) {
            log.debug(e);
        }

        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);

        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        formatterConfigurator.createControl(composite, ddlGenerator, () -> {});
        Composite serviceComposite = UIUtils.createComposite(composite, 2);
        serviceComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

        performDefaults();

        return composite;
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    protected boolean hasAccessToPage() {
        return DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER);
    }
}
