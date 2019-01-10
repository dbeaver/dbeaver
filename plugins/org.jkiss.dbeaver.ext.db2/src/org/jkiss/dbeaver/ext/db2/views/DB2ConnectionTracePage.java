/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.views;

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
        setTitle("Trace settings");
        setDescription("Trace settings");
        levels = new LevelConfig[] {
            new LevelConfig(DB2Constants.TRACE_CONNECTION_CALLS, "Connection calls"),
            new LevelConfig(DB2Constants.TRACE_STATEMENT_CALLS, "Statement calls"),
            new LevelConfig(DB2Constants.TRACE_RESULT_SET_CALLS, "Result set calls"),
            new LevelConfig(DB2Constants.TRACE_DRIVER_CONFIGURATION, "Driver configuration"),
            new LevelConfig(DB2Constants.TRACE_CONNECTS, "Connects"),
            new LevelConfig(DB2Constants.TRACE_DRDA_FLOWS, "DRDA flows"),
            new LevelConfig(DB2Constants.TRACE_RESULT_SET_META_DATA, "Result set metadata"),
            new LevelConfig(DB2Constants.TRACE_PARAMETER_META_DATA, "Parameter metadata"),
            new LevelConfig(DB2Constants.TRACE_DIAGNOSTICS, "Diagnostics"),
            new LevelConfig(DB2Constants.TRACE_SQLJ, "SQL J"),
            new LevelConfig(DB2Constants.TRACE_XA_CALLS, "XA calls"),
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

        enableTraceCheck = UIUtils.createCheckbox(cfgGroup, "Enable trace", false);

        traceGroup = new Composite(cfgGroup, SWT.NONE);
        traceGroup.setLayout(new GridLayout(2, false));
        traceGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        folderText = DialogUtils.createOutputFolderChooser(traceGroup, "Folder", null);
        fileNameText = UIUtils.createLabelText(traceGroup, "File name", "trace");
        traceAppendCheck = UIUtils.createLabelCheckbox(traceGroup, "Append", false);

        Group levelsGroup = UIUtils.createControlGroup(traceGroup, "Levels", 2, 0, 0);
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
            traceEnableState = ControlEnableState.disable(traceGroup);
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
