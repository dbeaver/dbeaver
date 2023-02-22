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
package org.jkiss.dbeaver.ui.editors.sql.ai.preferences;

import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.ai.AISettings;
import org.jkiss.dbeaver.model.ai.gpt3.GPTCompletionEngine;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.io.IOException;

public class AIPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage {
    private static final Log log = Log.getLog(AIPreferencePage.class);
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.ai";
    private final AISettings settings;

    private IObjectPropertyConfigurator<GPTCompletionEngine, AISettings> configurator;

    public AIPreferencePage() {
        UIPropertyConfiguratorDescriptor cfgDescriptor = UIPropertyConfiguratorRegistry.getInstance().getDescriptor(GPTCompletionEngine.class.getName());
        if (cfgDescriptor != null) {
            try {
                configurator = (IObjectPropertyConfigurator)cfgDescriptor.createConfigurator();
            } catch (DBException e) {
                log.error(e);
            }
        }
        if (configurator == null) {
            configurator = new AIConfiguratorDefault();
        }
        settings = AISettings.getSettings();
    }

    @Override
    protected void performDefaults() {
        configurator.loadSettings(settings);
    }

    @Override
    public boolean performOk() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        configurator.saveSettings(settings);

        try {
            store.save();
        } catch (IOException e) {
            log.debug(e);
        }

        if (DBWorkbench.getPlatform().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_CONFIGURATION_MANAGER)) {
            settings.saveSettings();
        }

        return true;
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite placeholder = UIUtils.createPlaceholder(parent, 1);
        placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));

        GPTCompletionEngine engine = new GPTCompletionEngine();
        configurator.createControl(placeholder, engine, () -> {});
        performDefaults();

        return placeholder;
    }

    @Override
    public void init(IWorkbench workbench) {

    }
}
