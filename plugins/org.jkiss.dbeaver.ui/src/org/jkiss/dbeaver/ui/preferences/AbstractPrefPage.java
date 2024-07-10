/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UIMessages;

/**
 * AbstractPrefPage
 */
public abstract class AbstractPrefPage extends PreferencePage {

    private static final Log log = Log.getLog(AbstractPrefPage.class);

    private IObjectPropertyConfigurator<Object, Object> extConfigurator;

    @Override
    protected Control createContents(Composite parent) {
        if (!hasAccessToPage()) {
            return UIUtils.createLabel(parent, UIMessages.preference_page_no_access);
        }
        final Control content = createPreferenceContent(parent);

        Dialog.applyDialogFont(content);

        return content;
    }

    void disableButtons() {
        noDefaultAndApplyButton();
    }

    @NotNull
    protected abstract Control createPreferenceContent(@NotNull Composite parent);

    protected boolean hasAccessToPage() {
        return true;
    }

    protected void restartWorkbenchOnPrefChange() {
        Runnable restarter = () -> PlatformUI.getWorkbench().restart();
        if (getContainer() instanceof Window window) {
            window.getShell().addDisposeListener(e ->
                UIUtils.asyncExec(restarter));
            window.close();
        } else {
            restarter.run();
        }
    }

    @Override
    protected void performApply() {
        if (extConfigurator != null) {
            extConfigurator.saveSettings(getConfiguratorObject());
        }
        super.performApply();
    }

    @Override
    public boolean performCancel() {
        return super.performCancel();
    }

    @Override
    protected void performDefaults() {
        if (extConfigurator != null) {
            extConfigurator.resetSettings(getConfiguratorObject());
        }
        super.performDefaults();
    }

    @Override
    public boolean performOk() {
        if (extConfigurator != null) {
            extConfigurator.saveSettings(getConfiguratorObject());
        }
        return super.performOk();
    }

    protected Object getConfiguratorObject() {
        return this;
    }

    protected void injectConfigurators(Composite composite) {
        UIPropertyConfiguratorDescriptor configDesc = UIPropertyConfiguratorRegistry.getInstance().getDescriptor(
            getConfiguratorObject());
        if (configDesc != null) {
            try {
                extConfigurator = configDesc.createConfigurator();
            } catch (Exception e) {
                log.error(e);
            }
        }
        if (extConfigurator != null) {
            extConfigurator.createControl(composite, getConfiguratorObject(), () -> {

            });
            extConfigurator.loadSettings(getConfiguratorObject());
        }
    }

}
