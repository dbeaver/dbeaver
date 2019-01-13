/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

/**
 * PostgreSourceViewEditor
 */
public class PostgreSourceViewEditor extends SQLSourceViewer<PostgreScriptObject> {
    
    private Boolean showPermissions;
    private boolean showColumnComments = true;

    public PostgreSourceViewEditor()
    {
    }

    @Override
    protected boolean isReadOnly()
    {
        PostgreScriptObject sourceObject = getSourceObject();
        if (sourceObject instanceof PostgreProcedure || sourceObject instanceof PostgreTrigger || sourceObject instanceof PostgreViewBase) {
            return false;
        }
        return true;
    }

    public boolean getShowPermissions() {
        // By default permissions enabled only for tables
        return showPermissions != null ? showPermissions : getSourceObject() instanceof PostgreTableBase;
    }

    @Override
    protected boolean isAnnotationRulerVisible() {
        return getSourceObject() instanceof PostgreProcedure;
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText)
    {
        getInputPropertySource().setPropertyValue(monitor, "objectDefinitionText", sourceText);
    }

    @Override
    protected void contributeEditorCommands(IContributionManager contributionManager)
    {
        super.contributeEditorCommands(contributionManager);
        PostgreScriptObject sourceObject = getSourceObject();
        if (sourceObject instanceof PostgreProcedure) {
            contributionManager.add(new Separator());
            contributionManager.add(ActionUtils.makeActionContribution(
                new Action("Show header", Action.AS_CHECK_BOX) {
                    {
                        setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TREE_PROCEDURE));
                        setToolTipText("Shows auto-generated function header");
                        setChecked(!isInDebugMode());
                    }
                    @Override
                    public void run() {
                        getDatabaseEditorInput().setAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE, !isChecked());
                        refreshPart(PostgreSourceViewEditor.this, true);
                    }
                }, true));
        }
        if (sourceObject instanceof PostgrePermissionsOwner) {
            contributionManager.add(ActionUtils.makeActionContribution(
                new Action("Show permissions", Action.AS_CHECK_BOX) {
                {
                    setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TREE_PERMISSIONS));
                    setToolTipText("Shows object permission grants");
                    setChecked(getShowPermissions());
                }
                @Override
                public void run() {
                    showPermissions = isChecked();
                    refreshPart(PostgreSourceViewEditor.this, true);
                }
            }, true));
        }
        if (sourceObject instanceof PostgreTableBase) {
            contributionManager.add(ActionUtils.makeActionContribution(
                new Action("Show comments", Action.AS_CHECK_BOX) {
                    {
                        setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TYPE_TEXT));
                        setToolTipText("Show column comments in table definition");
                        setChecked(showColumnComments);
                    }
                    @Override
                    public void run() {
                        showColumnComments = isChecked();
                        refreshPart(PostgreSourceViewEditor.this, true);
                    }
                }, true));
        }
    }

    @Override
    protected Map<String, Object> getSourceOptions() {
        Map<String, Object> options = super.getSourceOptions();
        boolean inDebug = isInDebugMode();
        options.put(DBPScriptObject.OPTION_DEBUGGER_SOURCE, inDebug);
        options.put(PostgreConstants.OPTION_DDL_SHOW_PERMISSIONS, getShowPermissions());
        options.put(PostgreConstants.OPTION_DDL_SHOW_COLUMN_COMMENTS, showColumnComments);
        return options;
    }

    private boolean isInDebugMode() {
        return CommonUtils.getBoolean(
            getDatabaseEditorInput().getAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE), false);
    }
}

