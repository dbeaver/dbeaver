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

package org.jkiss.dbeaver.ext.kingbase.ui.config;


import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.kingbase.KingbaseMessages;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseDataSource;
import org.jkiss.dbeaver.ext.kingbase.model.plan.KingbaseQueryPlaner;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UITask;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;


public class KingbaseExplainPlanConfigurator implements DBEObjectConfigurator<DBCQueryPlannerConfiguration> {

    // It is static as we want to save it between plan explains
    private static boolean analyse;
    private static boolean verbose;
    private static boolean costs = true;
    private static boolean settings;
    private static boolean buffers;
    private static boolean wal;
    private static boolean timing = true;

    private static KingbaseDataSource dataSource;

    @Override
    public DBCQueryPlannerConfiguration configureObject(@NotNull DBRProgressMonitor monitor, @Nullable DBECommandContext commandContext, @Nullable Object container, @NotNull DBCQueryPlannerConfiguration configuration, @NotNull Map<String, Object> options) {
        if (container instanceof DBCQueryPlanner) {
            DBPDataSource dbpDataSource = ((DBCQueryPlanner) container).getDataSource();
            if (dbpDataSource instanceof KingbaseDataSource) {
                dataSource = (KingbaseDataSource) dbpDataSource;
            }
        }
        return new UITask<DBCQueryPlannerConfiguration>() {
            @Override
            protected DBCQueryPlannerConfiguration runTask() {
                PlanConfigDialog dialog = new PlanConfigDialog();
                if (dialog.open() == IDialogConstants.OK_ID) {
                    Map<String, Object> parameters = configuration.getParameters();
                    parameters.put(KingbaseQueryPlaner.PARAM_ANALYSE, analyse);
                    parameters.put(KingbaseQueryPlaner.PARAM_VERBOSE, verbose);
                    
                    parameters.put(KingbaseQueryPlaner.PARAM_COSTS, costs);
                    parameters.put(KingbaseQueryPlaner.PARAM_BUFFERS, buffers);
                    
                    
                    parameters.put(KingbaseQueryPlaner.PARAM_SETTINGS, settings);
                    
                    
                    parameters.put(KingbaseQueryPlaner.PARAM_WAL, wal);
                    
                   
                    parameters.put(KingbaseQueryPlaner.PARAM_TIMING, timing);
                    
                    return configuration;
                }
                return null;
            }
        }.execute();
    }


    private static class PlanConfigDialog extends BaseDialog {

        private Button walCheckbox;
        private Button timingCheckbox;
        private Button buffersCheckbox;

        public PlanConfigDialog() {
            super(UIUtils.getActiveWorkbenchShell(), KingbaseMessages.dialog_query_planner_settings_title, null);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite dialogArea = super.createDialogArea(parent);
            
            Group settingsGroup = UIUtils.createControlGroup(
                dialogArea,
                KingbaseMessages.dialog_query_planner_settings_control_label,
                2,
                GridData.FILL_BOTH,
                0);
            Button analyseCheckbox = UIUtils.createCheckbox(
                settingsGroup,
                KingbaseMessages.dialog_query_planner_settings_analyze,
                KingbaseMessages.dialog_query_planner_settings_analyze_tip,
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
                    if (buffersCheckbox != null) {
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
                KingbaseMessages.dialog_query_planner_settings_verbose,
                KingbaseMessages.dialog_query_planner_settings_verbose_tip,
                verbose,
                2);
            verboseCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    verbose = verboseCheckbox.getSelection();
                }
            });

            
            Button costsCheckbox = UIUtils.createCheckbox(
                settingsGroup,
                KingbaseMessages.dialog_query_planner_settings_costs,
                KingbaseMessages.dialog_query_planner_settings_costs_tip,
                costs,
                2);
            costsCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    costs = costsCheckbox.getSelection();
                }
            });
            

            
            Button settingsCheckbox = UIUtils.createCheckbox(
                settingsGroup,
                KingbaseMessages.dialog_query_planner_settings,
                KingbaseMessages.dialog_query_planner_settings_tip,
                settings,
                2);
            settingsCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings = settingsCheckbox.getSelection();
                }
            });
            

            
            buffersCheckbox = UIUtils.createCheckbox(
                settingsGroup,
                KingbaseMessages.dialog_query_planner_settings_buffers,
                KingbaseMessages.dialog_query_planner_settings_buffers_tip,
                buffers,
                2);
            buffersCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    buffers = buffersCheckbox.getSelection();
                }
            });
            
                buffersCheckbox.setEnabled(analyseCheckbox.getSelection());
            

          
           
            

          
            timingCheckbox = UIUtils.createCheckbox(
                settingsGroup,
                KingbaseMessages.dialog_query_planner_settings_timing,
                KingbaseMessages.dialog_query_planner_settings_timing_tip,
                timing,
                2);
            timingCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    timing = timingCheckbox.getSelection();
                }
            });
            timingCheckbox.setEnabled(analyseCheckbox.getSelection());
            

            return dialogArea;
        }
    }

}
