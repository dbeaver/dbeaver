/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.views;

import com.ibm.db2.jcc.DB2BaseDataSource;
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
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
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
            new LevelConfig(DB2BaseDataSource.TRACE_CONNECTION_CALLS, "Connection calls"),
            new LevelConfig(DB2BaseDataSource.TRACE_STATEMENT_CALLS, "Statement calls"),
            new LevelConfig(DB2BaseDataSource.TRACE_RESULT_SET_CALLS, "Result set calls"),
            new LevelConfig(DB2BaseDataSource.TRACE_DRIVER_CONFIGURATION, "Driver configuration"),
            new LevelConfig(DB2BaseDataSource.TRACE_CONNECTS, "Connects"),
            new LevelConfig(DB2BaseDataSource.TRACE_DRDA_FLOWS, "DRDA flows"),
            new LevelConfig(DB2BaseDataSource.TRACE_RESULT_SET_META_DATA, "Result set metadata"),
            new LevelConfig(DB2BaseDataSource.TRACE_PARAMETER_META_DATA, "Parameter metadata"),
            new LevelConfig(DB2BaseDataSource.TRACE_DIAGNOSTICS, "Diagnostics"),
            new LevelConfig(DB2BaseDataSource.TRACE_SQLJ, "SQL J"),
            new LevelConfig(DB2BaseDataSource.TRACE_XA_CALLS, "XA calls"),
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

        folderText = UIUtils.createOutputFolderChooser(traceGroup, "Folder", null);
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
        DBPConnectionInfo connectionInfo = site.getActiveDataSource().getConnectionInfo();
        Map<Object,Object> connectionProperties = connectionInfo.getProperties();

        // Settings
        enableTraceCheck.setSelection(
            CommonUtils.getBoolean(
                connectionProperties.get(DB2Constants.PROP_TRACE_ENABLED), false));
        if (!enableTraceCheck.getSelection()) {
            traceEnableState = ControlEnableState.disable(traceGroup);
        }
        if (connectionProperties.containsKey(DB2Constants.PROP_TRACE_FOLDER)) {
            folderText.setText(
                CommonUtils.toString(
                    connectionProperties.get(DB2Constants.PROP_TRACE_FOLDER)));
        }
        if (connectionProperties.containsKey(DB2Constants.PROP_TRACE_FILE)) {
            fileNameText.setText(
                CommonUtils.toString(
                    connectionProperties.get(DB2Constants.PROP_TRACE_FILE)));
        }
        traceAppendCheck.setSelection(
            CommonUtils.getBoolean(
                connectionProperties.get(DB2Constants.PROP_TRACE_APPEND), false));
        int traceLevel = CommonUtils.toInt(
            connectionProperties.get(DB2Constants.PROP_TRACE_LEVEL));
        for (LevelConfig level : levels) {
            level.checkbox.setSelection((traceLevel & level.level) != 0);
        }
    }

    @Override
    public void saveSettings(DataSourceDescriptor dataSource)
    {
        super.saveSettings(dataSource);
        Map<Object, Object> connectionProperties = dataSource.getConnectionInfo().getProperties();

        {
            connectionProperties.put(DB2Constants.PROP_TRACE_ENABLED, enableTraceCheck.getSelection());
            connectionProperties.put(DB2Constants.PROP_TRACE_FOLDER, folderText.getText());
            connectionProperties.put(DB2Constants.PROP_TRACE_FILE, fileNameText.getText());
            connectionProperties.put(DB2Constants.PROP_TRACE_APPEND, traceAppendCheck.getSelection());
            int traceLevel = 0;
            for (LevelConfig level : levels) {
                if (level.checkbox.getSelection()) {
                    traceLevel |= level.level;
                }
            }
            connectionProperties.put(DB2Constants.PROP_TRACE_LEVEL, traceLevel);
        }
        saveConnectionURL(dataSource.getConnectionInfo());
    }

}
