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

package org.jkiss.dbeaver.ext.postgresql.ui.editors;

import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * PostgreSourceViewEditor
 */
public class PostgreSourceViewEditor extends SQLSourceViewer<PostgreScriptObject> {
    
    private Button showHeaderCheck;
    private Boolean showPermissions;
    private boolean showColumnComments = true;

    public PostgreSourceViewEditor()
    {
    }

    @Override
    protected boolean isReadOnly()
    {
        PostgreScriptObject sourceObject = getSourceObject();
        if (sourceObject instanceof PostgreProcedure || sourceObject instanceof PostgreTrigger) {
            return false;
        }
        return true;
    }

    public boolean getShowPermissions() {
        // By default permissions enabled only for tables
        return showPermissions != null ? showPermissions : getSourceObject() instanceof PostgreTableBase;
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText)
    {
        getEditorInput().getPropertySource().setPropertyValue(monitor, "objectDefinitionText", sourceText);
    }

    @Override
    protected void contributeEditorCommands(IContributionManager contributionManager)
    {
        super.contributeEditorCommands(contributionManager);
        PostgreScriptObject sourceObject = getSourceObject();
        if (sourceObject instanceof PostgreProcedure) {
            contributionManager.add(new Separator());
            contributionManager.add(new ControlContribution("ProcedureDebugSource") {
                @Override
                protected Control createControl(Composite parent) {
                    Composite ph = UIUtils.createPlaceholder(parent, 1, 0);
                    showHeaderCheck = UIUtils.createCheckbox(ph, "Show header", "Shows auto-generated function header", false, 0);
                    boolean showHeader = CommonUtils.getBoolean(
                        getEditorInput().getAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE), false);
                    showHeaderCheck.setSelection(showHeader);
                    showHeaderCheck.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            getEditorInput().setAttribute(
                                DBPScriptObject.OPTION_DEBUGGER_SOURCE,
                                showHeaderCheck.getSelection());
                            refreshPart(PostgreSourceViewEditor.this, true);
                        }
                    });
                    return ph;
                }
            });
        }
        if (sourceObject instanceof PostgrePermissionsOwner) {
            contributionManager.add(new ControlContribution("PGDDLShowPermissions") {
                @Override
                protected Control createControl(Composite parent) {
                    Composite ph = UIUtils.createPlaceholder(parent, 1, 0);
                    Button showPermissionsCheck = UIUtils.createCheckbox(ph, "Show permissions", "Show permission grants", getShowPermissions(), 0);
                    showPermissionsCheck.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            showPermissions = showPermissionsCheck.getSelection();
                            refreshPart(PostgreSourceViewEditor.this, true);
                        }
                    });
                    return ph;
                }
            });
        }
        if (sourceObject instanceof PostgreTableBase) {
            contributionManager.add(new ControlContribution("PGDDLShowColumnComments") {
                @Override
                protected Control createControl(Composite parent) {
                    Button showColumnCommentsCheck = UIUtils.createCheckbox(parent, "Show comments", "Show column comments in column definition", true, 0);
                    showColumnCommentsCheck.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            showColumnComments = showColumnCommentsCheck.getSelection();
                            refreshPart(PostgreSourceViewEditor.this, true);
                        }
                    });
                    return showColumnCommentsCheck;
                }
            });
        }
    }

    @Override
    protected Map<String, Object> getSourceOptions() {
        Map<String, Object> options = super.getSourceOptions();
        boolean inDebug = !CommonUtils.getBoolean(
            getEditorInput().getAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE), false);
        options.put(DBPScriptObject.OPTION_DEBUGGER_SOURCE, inDebug);
        options.put(PostgreConstants.OPTION_DDL_SHOW_PERMISSIONS, getShowPermissions());
        options.put(PostgreConstants.OPTION_DDL_SHOW_COLUMN_COMMENTS, showColumnComments);
        return options;
    }
}

