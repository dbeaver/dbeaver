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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseWizard;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;

import java.util.List;

public class PrefPageDataSourceAI extends AbstractPrefPage implements IWorkbenchPropertyPage {
    private IAdaptable element;
    private List<AIConnectionSettingsSection> sections = List.of();

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite content = UIUtils.createComposite(parent, 1);
        DBPDataSourceContainer connection = getConnection();
        if (connection != null) {
            DBRRunnableContext runnableContext = getContainer() instanceof IWizardContainer wc
                && wc.getCurrentPage().getWizard() instanceof BaseWizard wizard
                ? wizard.getRunnableContext() : null;
            sections = AIConnectionSettingsRegistry.getInstance().getSections(connection);
            for (AIConnectionSettingsSection section : sections) {
                section.createControls(content, connection, runnableContext);
            }
        }
        return content;
    }

    @Override
    public boolean performOk() {
        saveSettings();
        return super.performOk();
    }

    @Override
    protected void performApply() {
        saveSettings();
        super.performApply();
    }

    private void saveSettings() {
        DBPDataSourceContainer connection = getConnection();
        if (connection != null) {
            for (AIConnectionSettingsSection section : sections) {
                section.save(connection);
            }
            if (!(getContainer() instanceof IWizardContainer)) {
                connection.persistConfiguration();
            }
        }
    }

    @Nullable
    private DBPDataSourceContainer getConnection() {
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
