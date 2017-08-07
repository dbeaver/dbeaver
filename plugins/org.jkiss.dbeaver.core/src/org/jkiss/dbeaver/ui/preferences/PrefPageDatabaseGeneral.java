/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFile;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.utils.SystemVariablesResolver;

/**
 * PrefPageDatabaseGeneral
 */
public class PrefPageDatabaseGeneral extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.common"; //$NON-NLS-1$

    private Button automaticUpdateCheck;

    private Button longOperationsCheck;
    private Spinner longOperationsTimeout;

    private Combo defaultResourceEncoding;

    private Button logsDebugEnabled;
    private TextWithOpenFile logsDebugLocation;

    public PrefPageDatabaseGeneral()
    {
        super();
        setPreferenceStore(new PreferenceStoreDelegate(DBeaverCore.getGlobalPreferenceStore()));
    }

    @Override
    public void init(IWorkbench workbench)
    {

    }

    @Override
    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group groupObjects = UIUtils.createControlGroup(composite, CoreMessages.pref_page_ui_general_group_general, 1, GridData.VERTICAL_ALIGN_BEGINNING, 300);
            automaticUpdateCheck = UIUtils.createCheckbox(groupObjects, CoreMessages.pref_page_ui_general_checkbox_automatic_updates, false);
            automaticUpdateCheck.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 2, 1));
        }

        // Agent settings
        {
            Group agentGroup = UIUtils.createControlGroup(composite, "Task Bar", 2, SWT.NONE, 0);

            longOperationsCheck = UIUtils.createCheckbox(agentGroup,
                    "Enable long-time operations notification",
                    "Shows special notification in system taskbar after long-time operation (e.g. SQL query) finish.", false, 2);

            longOperationsTimeout = UIUtils.createLabelSpinner(agentGroup, "Long-time operation timeout", 0, 0, Integer.MAX_VALUE);

            if (RuntimeUtils.isPlatformMacOS()) {
                ControlEnableState.disable(agentGroup);
            }
        }

        {
            // Resources
            Group groupResources = UIUtils.createControlGroup(composite, "Resources", 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);

            UIUtils.createControlLabel(groupResources, "Default resource encoding");
            defaultResourceEncoding = UIUtils.createEncodingCombo(groupResources, GeneralUtils.DEFAULT_ENCODING);
            defaultResourceEncoding.setToolTipText("Default encoding for scripts and text files. Change requires restart");

        }

        {
            // Logs
            Group groupLogs = UIUtils.createControlGroup(composite, "Debug logs", 2, GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING, 0);

            logsDebugEnabled = UIUtils.createCheckbox(groupLogs,
                    "Enable debug logs",
                    "Debug logs can be used for DBeaver itself debugging. Also used to store all errors/warnings/messages", false, 2);
            UIUtils.createControlLabel(groupLogs, "Log file location");
            logsDebugLocation = new TextWithOpenFile(groupLogs, "Debug log file location", new String[] { "*.log", "*.txt" } );
            UIUtils.installContentProposal(
                    logsDebugLocation.getTextControl(),
                    new TextContentAdapter(),
                    new SimpleContentProposalProvider(new String[] {
                            GeneralUtils.variablePattern(SystemVariablesResolver.VAR_WORKSPACE),
                            GeneralUtils.variablePattern(SystemVariablesResolver.VAR_HOME)
                    }));
            logsDebugLocation.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Label tipLabel = UIUtils.createLabel(groupLogs, "These options will take effect after DBeaver restart");
            tipLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false , 2, 1));
        }

        {
            // Link to secure storage config
            new PreferenceLinkArea(composite, SWT.NONE,
                PrefPageEntityEditor.PAGE_ID,
                "<a>''{0}''</a> settings",
                (IWorkbenchPreferenceContainer) getContainer(), null); //$NON-NLS-1$

            new PreferenceLinkArea(composite, SWT.NONE,
                PrefPageSQLEditor.PAGE_ID,
                "<a>''{0}''</a> settings",
                (IWorkbenchPreferenceContainer) getContainer(), null); //$NON-NLS-1$

        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        automaticUpdateCheck.setSelection(store.getBoolean(DBeaverPreferences.UI_AUTO_UPDATE_CHECK));
        longOperationsCheck.setSelection(store.getBoolean(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY));
        longOperationsTimeout.setSelection(store.getInt(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT));

        defaultResourceEncoding.setText(store.getString(DBeaverPreferences.DEFAULT_RESOURCE_ENCODING));

        logsDebugEnabled.setSelection(store.getBoolean(DBeaverPreferences.LOGS_DEBUG_ENABLED));
        logsDebugLocation.setText(store.getString(DBeaverPreferences.LOGS_DEBUG_LOCATION));
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();

        store.setValue(DBeaverPreferences.UI_AUTO_UPDATE_CHECK, automaticUpdateCheck.getSelection());
        //store.setValue(DBeaverPreferences.AGENT_ENABLED, agentEnabledCheck.getSelection());
        store.setValue(DBeaverPreferences.AGENT_LONG_OPERATION_NOTIFY, longOperationsCheck.getSelection());
        store.setValue(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT, longOperationsTimeout.getSelection());

        store.setValue(DBeaverPreferences.DEFAULT_RESOURCE_ENCODING, defaultResourceEncoding.getText());

        store.setValue(DBeaverPreferences.LOGS_DEBUG_ENABLED, logsDebugEnabled.getSelection());
        store.setValue(DBeaverPreferences.LOGS_DEBUG_LOCATION, logsDebugLocation.getText());

        PrefUtils.savePreferenceStore(store);

        return true;
    }

    @Nullable
    @Override
    public IAdaptable getElement()
    {
        return null;
    }

    @Override
    public void setElement(IAdaptable element)
    {
    }

}