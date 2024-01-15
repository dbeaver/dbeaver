/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
 * Copyright (C) 2019 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.postgresql.ui.config;


import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.plan.PostgreQueryPlaner;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.Map;


public class PostgreExplainPlanConfigurator implements DBEObjectConfigurator<DBCQueryPlannerConfiguration> {

    // It is static as we want to save it between plan explains
    private static boolean analyse;
    private static boolean verbose;
    private static boolean costs = true;
    private static boolean settings;
    private static boolean buffers;
    private static boolean wal;
    private static boolean timing = true;

    private static PostgreDataSource dataSource;

    @Override
    public DBCQueryPlannerConfiguration configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object container, @NotNull DBCQueryPlannerConfiguration configuration, @NotNull Map<String, Object> options) {
        if (container instanceof DBCQueryPlanner) {
            DBPDataSource dbpDataSource = ((DBCQueryPlanner) container).getDataSource();
            if (dbpDataSource instanceof PostgreDataSource) {
                dataSource = (PostgreDataSource) dbpDataSource;
            }
        }
        return new UITask<DBCQueryPlannerConfiguration>() {
            @Override
            protected DBCQueryPlannerConfiguration runTask() {
                PlanConfigDialog dialog = new PlanConfigDialog();
                if (dialog.open() == IDialogConstants.OK_ID) {
                    Map<String, Object> parameters = configuration.getParameters();
                    parameters.put(PostgreQueryPlaner.PARAM_ANALYSE, analyse);
                    parameters.put(PostgreQueryPlaner.PARAM_VERBOSE, verbose);
                    if (isVersionSupports(9, 0)) {
                        parameters.put(PostgreQueryPlaner.PARAM_COSTS, costs);
                        parameters.put(PostgreQueryPlaner.PARAM_BUFFERS, buffers);
                    }
                    if (isVersionSupports(12, 0)) {
                        parameters.put(PostgreQueryPlaner.PARAM_SETTINGS, settings);
                    }
                    if (isVersionSupports(13, 0)) {
                        parameters.put(PostgreQueryPlaner.PARAM_WAL, wal);
                    }
                    if (isVersionSupports(9, 2)) {
                        parameters.put(PostgreQueryPlaner.PARAM_TIMING, timing);
                    }
                    /*if (isVersionSupports(10, 0)) {
                        parameters.put(PostgreQueryPlaner.PARAM_SUMMARY, summary);
                    }*/
                    return configuration;
                }
                return null;
            }
        }.execute();
    }

    private static boolean isVersionSupports(int major, int minor) {
        return dataSource != null && dataSource.isServerVersionAtLeast(major, minor);
    }

    private static class PlanConfigDialog extends BaseDialog {

        private Button walCheckbox;
        private Button timingCheckbox;
        private Button buffersCheckbox;

        public PlanConfigDialog() {
            super(UIUtils.getActiveWorkbenchShell(), PostgreMessages.dialog_query_planner_settings_title, null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite dialogArea = super.createDialogArea(parent);
            boolean isServerAtLeast13 = dataSource != null && dataSource.isServerVersionAtLeast(13, 0);
            boolean isServerAtLeast9 = dataSource != null && dataSource.isServerVersionAtLeast(9, 0);
            Group settingsGroup = UIUtils.createControlGroup(
                dialogArea,
                PostgreMessages.dialog_query_planner_settings_control_label,
                2,
                GridData.FILL_BOTH,
                0);
            Button analyseCheckbox = UIUtils.createCheckbox(
                settingsGroup,
                PostgreMessages.dialog_query_planner_settings_analyze,
                PostgreMessages.dialog_query_planner_settings_analyze_tip,
                analyse,
                2);
            analyseCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    boolean analyseCheckboxSelection = analyseCheckbox.getSelection();
                    analyse = analyseCheckbox.getSelection();
                    if (walCheckbox != null) {
                        walCheckbox.setEnabled(analyseCheckboxSelection);
                        if (walCheckbox.getSelection() && !analyseCheckboxSelection) {
                            walCheckbox.setSelection(false);
                            wal = false;
                        }
                    }
                    if (timingCheckbox != null) {
                        timingCheckbox.setEnabled(analyseCheckboxSelection);
                        if (!analyseCheckboxSelection) {
                            timing = false;
                        } else if (timingCheckbox.getSelection() && !timing) {
                            timing = true;
                        }
                    }
                    /*if (summaryCheckbox != null && analyseCheckboxSelection) {
                        // SUMMARY has default value for ANALYZE parameter as true
                        summaryCheckbox.setSelection(true);
                        summary = true;
                    }*/
                    if (buffersCheckbox != null && !isServerAtLeast13) {
                        buffersCheckbox.setEnabled(analyseCheckboxSelection);
                        if (buffersCheckbox.getSelection() && !analyseCheckboxSelection) {
                            buffersCheckbox.setSelection(false);
                            buffers = false;
                        }
                    }
                }
            });

            Button verboseCheckbox = UIUtils.createCheckbox(
                settingsGroup,
                PostgreMessages.dialog_query_planner_settings_verbose,
                PostgreMessages.dialog_query_planner_settings_verbose_tip,
                verbose,
                2);
            verboseCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    verbose = verboseCheckbox.getSelection();
                }
            });

            if (isServerAtLeast9) {
                Button costsCheckbox = UIUtils.createCheckbox(
                    settingsGroup,
                    PostgreMessages.dialog_query_planner_settings_costs,
                    PostgreMessages.dialog_query_planner_settings_costs_tip,
                    costs,
                    2);
                costsCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        costs = costsCheckbox.getSelection();
                    }
                });
            }

            if (isVersionSupports(12, 0)) {
                Button settingsCheckbox = UIUtils.createCheckbox(
                    settingsGroup,
                    PostgreMessages.dialog_query_planner_settings,
                    PostgreMessages.dialog_query_planner_settings_tip,
                    settings,
                    2);
                settingsCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings = settingsCheckbox.getSelection();
                    }
                });
            }

            if (isServerAtLeast9) {
                buffersCheckbox = UIUtils.createCheckbox(
                    settingsGroup,
                    PostgreMessages.dialog_query_planner_settings_buffers,
                    PostgreMessages.dialog_query_planner_settings_buffers_tip,
                    buffers,
                    2);
                buffersCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        buffers = buffersCheckbox.getSelection();
                    }
                });
                if (!isServerAtLeast13) {
                    buffersCheckbox.setEnabled(analyseCheckbox.getSelection());
                }
            }

            if (isServerAtLeast13) {
                walCheckbox = UIUtils.createCheckbox(
                    settingsGroup,
                    PostgreMessages.dialog_query_planner_settings_wal,
                    PostgreMessages.dialog_query_planner_settings_wal_tip,
                    wal,
                    2);
                walCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        wal = walCheckbox.getSelection();
                    }
                });
                walCheckbox.setEnabled(analyseCheckbox.getSelection());
            }

            if (isVersionSupports(9, 2)) {
                timingCheckbox = UIUtils.createCheckbox(
                    settingsGroup,
                    PostgreMessages.dialog_query_planner_settings_timing,
                    PostgreMessages.dialog_query_planner_settings_timing_tip,
                    timing,
                    2);
                timingCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        timing = timingCheckbox.getSelection();
                    }
                });
                timingCheckbox.setEnabled(analyseCheckbox.getSelection());
            }

            // Summary needs special support. Maybe we will add it some day.
            /*if (isVersionSupports(10, 0)) {
                summaryCheckbox = UIUtils.createCheckbox(
                    settingsGroup,
                    PostgreMessages.dialog_query_planner_settings_summary,
                    PostgreMessages.dialog_query_planner_settings_summary_tip,
                    summary,
                    2);
                summaryCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        summary = summaryCheckbox.getSelection();
                    }
                });
            }*/

            return dialogArea;
        }
    }

}
