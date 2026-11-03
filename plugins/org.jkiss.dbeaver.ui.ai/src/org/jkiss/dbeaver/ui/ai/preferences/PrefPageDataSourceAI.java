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
package org.jkiss.dbeaver.ui.ai.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.ai.AIContextSettingsDataSource;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

public class PrefPageDataSourceAI extends AbstractPrefPage implements IWorkbenchPropertyPage {
    private static final Log log = Log.getLog(PrefPageDataSourceAI.class);

    private IAdaptable element;
    private IObjectPropertyConfigurator<AIContextSettingsDataSource, PrefPageDataSourceAI> aiConfigurator;

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite content = UIUtils.createComposite(parent, 1);
        DBPDataSourceContainer connection = getConnection();
        if (connection != null) {
            injectConfigurators(content);
            UIPropertyConfiguratorDescriptor descriptor = UIPropertyConfiguratorRegistry.getInstance()
                .getDescriptor(AIContextSettingsDataSource.class.getName());
            if (descriptor != null) {
                try {
                    IObjectPropertyConfigurator<AIContextSettingsDataSource, PrefPageDataSourceAI> configurator =
                        descriptor.createConfigurator();
                    configurator.createControl(content, new AIContextSettingsDataSource(connection), () -> {});
                    configurator.loadSettings(this);
                    aiConfigurator = configurator;
                } catch (DBException e) {
                    log.error("Cannot load AI connection settings", e);
                }
            }
        }
        return content;
    }

    @Override
    public boolean performOk() {
        boolean result = super.performOk();
        if (result) {
            saveAISettings();
            persistConnection();
        }
        return result;
    }

    @Override
    protected void performApply() {
        super.performApply();
        saveAISettings();
        persistConnection();
    }

    private void saveAISettings() {
        if (aiConfigurator != null) {
            aiConfigurator.saveSettings(this);
        }
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        if (aiConfigurator != null) {
            aiConfigurator.resetSettings(this);
        }
    }

    private void persistConnection() {
        DBPDataSourceContainer connection = getConnection();
        if (connection != null && !(getContainer() instanceof IWizardContainer)) {
            connection.persistConfiguration();
        }
    }

    @Nullable
    public DBPDataSourceContainer getConnection() {
        return element == null ? null : element.getAdapter(DBPDataSourceContainer.class);
    }

    @Override
    public IAdaptable getElement() {
        return element;
    }

    @Override
    public void setElement(IAdaptable element) {
        this.element = element;
    }
}
