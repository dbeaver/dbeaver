/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.format;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.registry.sql.SQLFormatterConfigurer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

public abstract class BaseFormatterConfigurationPage extends ActiveWizardPage implements SQLFormatterConfigurer {

    public BaseFormatterConfigurationPage()
    {
        super("SQL Format");
    }

    @Override
    public void performHelp() {
        super.performHelp();
    }

    @Override
    public final void createControl(Composite parent) {
        setTitle("SQL Format Configuration");
        setDescription(getWizard().getFormaterName());
        Composite composite = createFormatSettings(parent, getWizard().getConfiguration());



        setControl(composite);
    }

    @Override
    public ConfigWizard getWizard() {
        return (ConfigWizard)super.getWizard();
    }

    protected abstract Composite createFormatSettings(Composite parent, SQLFormatterConfiguration configuration);

    protected abstract void saveFormatSettings(SQLFormatterConfiguration configuration);

    @Override
    public boolean configure(String formatName, SQLFormatter formatter, SQLFormatterConfiguration configuration) {
        Wizard wizard = new ConfigWizard(formatName, configuration);
        wizard.addPage(this);
        WizardDialog configDialog = new WizardDialog(DBeaverUI.getActiveWorkbenchShell(), wizard) {
            @Override
            protected void createButtonsForButtonBar(Composite parent) {
                super.createButtonsForButtonBar(parent);
                UIUtils.createCheckbox(parent, "Don't show again", false);
                ((GridLayout)parent.getLayout()).numColumns++;
            }
        };
        return configDialog.open() == IDialogConstants.OK_ID;
    }

    protected static class ConfigWizard extends Wizard {
        private final SQLFormatterConfiguration configuration;
        private String formaterName;

        public ConfigWizard(String formaterName, SQLFormatterConfiguration configuration) {
            setWindowTitle("SQL Format");
            this.formaterName = formaterName;
            this.configuration = configuration;
        }

        public SQLFormatterConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        public boolean performFinish() {
            for (IWizardPage page : getPages()) {
                if (page instanceof BaseFormatterConfigurationPage) {
                    ((BaseFormatterConfigurationPage) page).saveFormatSettings(this.configuration);
                }
            }
            return true;
        }

        public String getFormaterName() {
            return formaterName;
        }
    }
}
