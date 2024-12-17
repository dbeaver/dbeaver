/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls.resultset.actions;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.hints.DBDValueHint;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.ui.IObjectPropertyConfigurator;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetViewer;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

public class HintConfigurationAction extends AbstractResultSetViewerAction {
    private static final Log log = Log.getLog(HintConfigurationAction.class);

    private final DBDAttributeBinding attr;
    private final DBDValueHint hint;
    private final UIPropertyConfiguratorDescriptor configDescriptor;

    public HintConfigurationAction(
        @NotNull ResultSetViewer resultSetViewer,
        @NotNull DBDAttributeBinding attr,
        @NotNull DBDValueHint hint,
        @NotNull UIPropertyConfiguratorDescriptor configDescriptor
    ) {
        super(resultSetViewer, hint.getHintDescription() + " ...");
        this.attr = attr;
        this.hint = hint;
        this.configDescriptor = configDescriptor;
        setToolTipText(hint.getHintDescription());
    }

    @Override
    public void run() {
        ConfigDialog dialog = new ConfigDialog(getResultSetViewer().getSite().getShell());
        if (dialog.open() == IDialogConstants.OK_ID) {
            getResultSetViewer().refreshData(null);
        }
    }

    private class ConfigDialog extends BaseDialog {

        private IObjectPropertyConfigurator<DBDValueHint, DBDAttributeBinding> configurator;

        public ConfigDialog(Shell parentShell) {
            super(parentShell, hint.getHintDescription(), null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite composite = super.createDialogArea(parent);
            try {
                configurator = HintConfigurationAction.this.configDescriptor.createConfigurator();
                configurator.createControl(composite, hint, () -> {});
                configurator.loadSettings(attr);
            } catch (Exception e) {
                log.error(e);
            }
            return composite;
        }

        @Override
        protected void okPressed() {
            configurator.saveSettings(attr);
            super.okPressed();
        }
    }

}
