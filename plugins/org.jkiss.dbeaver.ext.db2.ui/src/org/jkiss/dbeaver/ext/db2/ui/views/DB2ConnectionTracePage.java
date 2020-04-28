/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.db2.ui.views;

import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.ui.internal.DB2Messages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.dialogs.connection.ConnectionPageAbstract;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * OracleConnectionPage
 */
public class DB2ConnectionTracePage extends ConnectionPageAbstract
{

    private Button enableTraceCheck;
    private Text fileNameText;
    private Text folderText;
    private Button traceAppendCheck;
    private LevelConfig[] levels;
    private ControlEnableState traceEnableState;
    private Composite traceGroup;

    private static class LevelConfig {
        final int level;
        final String label;
        Button checkbox;

        private LevelConfig(int level, String label)
        {
            this.level = level;
            this.label = label;
        }
    }

    public DB2ConnectionTracePage()
    {
        setTitle(DB2Messages.db2_connection_trace_page_tab_trace_settings);
        setDescription(DB2Messages.db2_connection_trace_page_tab_description_trace_settings);
        levels = new LevelConfig[] {
            new LevelConfig(DB2Constants.TRACE_CONNECTION_CALLS, DB2Messages.db2_connection_trace_page_checkbox_connection_calls),
            new LevelConfig(DB2Constants.TRACE_STATEMENT_CALLS, DB2Messages.db2_connection_trace_page_checkbox_statement_calls),
            new LevelConfig(DB2Constants.TRACE_RESULT_SET_CALLS, DB2Messages.db2_connection_trace_page_checkbox_result_set_calls),
            new LevelConfig(DB2Constants.TRACE_DRIVER_CONFIGURATION, DB2Messages.db2_connection_trace_page_checkbox_driver_configuration),
            new LevelConfig(DB2Constants.TRACE_CONNECTS, DB2Messages.db2_connection_trace_page_checkbox_connect),
            new LevelConfig(DB2Constants.TRACE_DRDA_FLOWS, DB2Messages.db2_connection_trace_page_checkbox_drda_flows),
            new LevelConfig(DB2Constants.TRACE_RESULT_SET_META_DATA, DB2Messages.db2_connection_trace_page_checkbox_result_set_metadata),
            new LevelConfig(DB2Constants.TRACE_PARAMETER_META_DATA, DB2Messages.db2_connection_trace_page_checkbox_parameter_metadata),
            new LevelConfig(DB2Constants.TRACE_DIAGNOSTICS, DB2Messages.db2_connection_trace_page_checkbox_diagnostics),
            new LevelConfig(DB2Constants.TRACE_SQLJ, DB2Messages.db2_connection_trace_page_checkbox_sql_j),
            new LevelConfig(DB2Constants.TRACE_XA_CALLS, DB2Messages.db2_connection_trace_page_checkbox_xa_calls),
        };
    }

    @Override
    public void dispose()
    {
        super.dispose();
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite cfgGroup = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.marginHeight = 10;
        gl.marginWidth = 10;
        cfgGroup.setLayout(gl);
        GridData gd = new GridData(GridData.FILL_BOTH);
        cfgGroup.setLayoutData(gd);

        enableTraceCheck = UIUtils.createCheckbox(cfgGroup, DB2Messages.db2_connection_trace_page_checkbox_enable_trace, false);

        traceGroup = new Composite(cfgGroup, SWT.NONE);
        traceGroup.setLayout(new GridLayout(2, false));
        traceGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        folderText = DialogUtils.createOutputFolderChooser(traceGroup, DB2Messages.db2_connection_trace_page_label_folder, null);
        fileNameText = UIUtils.createLabelText(traceGroup, DB2Messages.db2_connection_trace_page_label_file_name, DB2Messages.db2_connection_trace_page_string_trace);
        traceAppendCheck = UIUtils.createLabelCheckbox(traceGroup, DB2Messages.db2_connection_trace_page_checkbox_append, false);

        Group levelsGroup = UIUtils.createControlGroup(traceGroup, DB2Messages.db2_connection_trace_page_header_levels, 2, 0, 0);
        gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
        gd.horizontalSpan = 2;
        levelsGroup.setLayoutData(gd);

        for (LevelConfig level : levels) {
            level.checkbox = UIUtils.createCheckbox(levelsGroup, level.label, false);
        }

        enableTraceCheck.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                if (traceEnableState == null) {
                    traceEnableState = ControlEnableState.disable(traceGroup);
                } else {
                    traceEnableState.restore();
                    traceEnableState = null;
                }
            }
        });

        setControl(cfgGroup);

        loadSettings();
    }

    @Override
    public boolean isComplete()
    {
        return true;
    }

    @Override
    public void loadSettings()
    {
        // Load values from new connection info
        DBPConnectionConfiguration connectionInfo = site.getActiveDataSource().getConnectionConfiguration();
        Map<String, String> providerProperties = connectionInfo.getProviderProperties();

        // Settings
        enableTraceCheck.setSelection(
            CommonUtils.getBoolean(
                providerProperties.get(DB2Constants.PROP_TRACE_ENABLED), false));
        if (!enableTraceCheck.getSelection()) {
            if (traceEnableState == null) {
                traceEnableState = ControlEnableState.disable(traceGroup);
            }
        }
        if (providerProperties.containsKey(DB2Constants.PROP_TRACE_FOLDER)) {
            folderText.setText(
                CommonUtils.toString(
                    providerProperties.get(DB2Constants.PROP_TRACE_FOLDER)));
        }
        if (providerProperties.containsKey(DB2Constants.PROP_TRACE_FILE)) {
            fileNameText.setText(
                CommonUtils.toString(
                    providerProperties.get(DB2Constants.PROP_TRACE_FILE)));
        }
        traceAppendCheck.setSelection(
            CommonUtils.getBoolean(
                providerProperties.get(DB2Constants.PROP_TRACE_APPEND), false));
        int traceLevel = CommonUtils.toInt(
            providerProperties.get(DB2Constants.PROP_TRACE_LEVEL));
        for (LevelConfig level : levels) {
            level.checkbox.setSelection((traceLevel & level.level) != 0);
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSource)
    {
        super.saveSettings(dataSource);
        Map<String, String> providerProperties = dataSource.getConnectionConfiguration().getProviderProperties();

        {
            providerProperties.put(DB2Constants.PROP_TRACE_ENABLED, String.valueOf(enableTraceCheck.getSelection()));
            providerProperties.put(DB2Constants.PROP_TRACE_FOLDER, folderText.getText());
            providerProperties.put(DB2Constants.PROP_TRACE_FILE, fileNameText.getText());
            providerProperties.put(DB2Constants.PROP_TRACE_APPEND, String.valueOf(traceAppendCheck.getSelection()));
            int traceLevel = 0;
            for (LevelConfig level : levels) {
                if (level.checkbox.getSelection()) {
                    traceLevel |= level.level;
                }
            }
            providerProperties.put(DB2Constants.PROP_TRACE_LEVEL, String.valueOf(traceLevel));
        }
        saveConnectionURL(dataSource.getConnectionConfiguration());
    }

}
