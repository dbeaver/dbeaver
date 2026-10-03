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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.ext.mimer.ui.internal.MimerUIMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

/**
 * Extra "Mimer SQL" connection wizard page for settings the generic connection page doesn't
 * know about: Protocol (TCP/IP or Local shared-memory IPC - stored as a provider property,
 * {@link MimerConstants#PROP_PROTOCOL}, since it changes the connection URL itself rather than
 * being a real JDBC property) and Program/Program password (Mimer SQL's {@code ENTER <program>
 * USING <password>} security layer - these <i>are</i> real JDBC properties the driver reads
 * directly). The program name is a plain property ({@link DBPConnectionConfiguration#setProperty})
 * - DBeaver auto-merges those into the JDBC connect properties for every data source. The
 * password is a genuine secret, so it goes through {@link DBPConnectionConfiguration#setAuthProperty}
 * instead, which routes it into DBeaver's secure credential storage rather than the plaintext
 * connection configuration - auth properties are *not* auto-merged, so {@code
 * org.jkiss.dbeaver.ext.mimer.model.MimerDataSource} explicitly injects it back into the JDBC
 * {@code Properties} at connect time.
 *
 * @author Mimer Information Technology
 */
public class MimerConnectionSettingsPage extends ConnectionPageAbstract {

    private static final String[] PROTOCOL_LABELS = {"TCP/IP", "Local (shared memory)"};

    private Combo protocolCombo;
    private Text programText;
    private Text programPasswordText;

    public MimerConnectionSettingsPage() {
        // "Advanced" matches MySQL's/PostgreSQL's own extra-settings page in the New
        // Connection wizard (MySQLConnectionPageAdvanced/PostgreConnectionPageAdvanced) -
        // the wizard already knows it's a Mimer SQL connection, so naming this page after
        // the driver again was redundant.
        setTitle("Advanced");
    }

    @Override
    public void createControl(@NotNull Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite protocolGroup = UIUtils.createTitledComposite(container, "Connection Protocol", 2, GridData.FILL_HORIZONTAL);
        UIUtils.createControlLabel(protocolGroup, "Protocol");
        protocolCombo = new Combo(protocolGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (String label : PROTOCOL_LABELS) {
            protocolCombo.add(label);
        }
        protocolCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        protocolCombo.setToolTipText(MimerUIMessages.page_connection_settings_protocol_tooltip);

        Composite programGroup = UIUtils.createTitledComposite(container, "Program", 2, GridData.FILL_HORIZONTAL);
        programGroup.setToolTipText(MimerUIMessages.page_connection_settings_program_tooltip);

        programText = UIUtils.createLabelText(programGroup, "Program", "");
        programPasswordText = UIUtils.createLabelText(programGroup, "Program Password", "", SWT.BORDER | SWT.PASSWORD);

        setControl(container);
        loadSettings();
    }

    @Override
    public void loadSettings() {
        if (protocolCombo == null) {
            return;
        }
        DBPConnectionConfiguration config = getSite().getActiveDataSource().getConnectionConfiguration();
        boolean local = MimerConstants.PROTOCOL_LOCAL.equals(config.getProviderProperty(MimerConstants.PROP_PROTOCOL));
        protocolCombo.select(local ? 1 : 0);
        programText.setText(CommonUtils.notEmpty(config.getProperty(MimerConstants.PROP_PROGRAM)));
        programPasswordText.setText(CommonUtils.notEmpty(config.getAuthProperty(MimerConstants.PROP_PROGRAM_PASSWORD)));
    }

    @Override
    public void saveSettings(@NotNull DBPDataSourceContainer dataSource) {
        if (protocolCombo == null) {
            return;
        }
        DBPConnectionConfiguration config = dataSource.getConnectionConfiguration();
        config.setProviderProperty(
            MimerConstants.PROP_PROTOCOL,
            protocolCombo.getSelectionIndex() == 1 ? MimerConstants.PROTOCOL_LOCAL : MimerConstants.PROTOCOL_TCP);

        String program = programText.getText().trim();
        config.setProperty(MimerConstants.PROP_PROGRAM, program.isEmpty() ? null : program);

        String programPassword = programPasswordText.getText();
        config.setAuthProperty(MimerConstants.PROP_PROGRAM_PASSWORD, programPassword.isEmpty() ? null : programPassword);
    }

    @Override
    public boolean isComplete() {
        return true;
    }
}
