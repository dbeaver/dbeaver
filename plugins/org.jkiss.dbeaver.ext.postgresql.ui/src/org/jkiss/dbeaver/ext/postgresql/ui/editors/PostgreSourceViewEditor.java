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

package org.jkiss.dbeaver.ext.postgresql.ui.editors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.ext.postgresql.PostgreMessages;
import org.jkiss.dbeaver.ext.postgresql.model.*;
import org.jkiss.dbeaver.ext.postgresql.ui.editors.sql.handlers.SQLEditorHandlerCheckProcedureConsole;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithResult;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenObjectConsole;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PostgreSourceViewEditor
 */
public class PostgreSourceViewEditor extends SQLSourceViewer<PostgreScriptObject> {

    public PostgreSourceViewEditor() {

    }

    @Override
    protected boolean isReadOnly()
    {
        PostgreScriptObject sourceObject = getSourceObject();
        if (sourceObject instanceof PostgreProcedure || sourceObject instanceof PostgreTriggerBase || sourceObject instanceof PostgreViewBase || sourceObject instanceof PostgreJobStep) {
            return false;
        }
        return true;
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
                new Action(PostgreMessages.source_view_show_header_label, Action.AS_CHECK_BOX) {
                    {
                        setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TREE_PROCEDURE));
                        setToolTipText(PostgreMessages.source_view_show_header_description);
                        setChecked(!isInDebugMode());
                    }
                    @Override
                    public void run() {
                        getDatabaseEditorInput().setAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE, !isChecked());
                        refreshPart(PostgreSourceViewEditor.this, true);
                    }
                }, true));
            contributionManager.add(ActionUtils.makeActionContribution(
                new Action(PostgreMessages.procedure_check_label, Action.AS_PUSH_BUTTON) {
                    {
                        setToolTipText(PostgreMessages.procedure_check_description);
                    }
                        
                    @Override
                    public void run() {
                        IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
                        List<DBSProcedure> entities = new ArrayList<>();
                        entities.add((DBSProcedure) sourceObject);
                        DBRRunnableWithResult<String> generator = SQLEditorHandlerCheckProcedureConsole.checkGenerator(entities);
                        UIUtils.runInUI(workbenchWindow, generator);
                        String sql = CommonUtils.notEmpty(generator.getResult());
                        SQLNavigatorContext navContext = new SQLNavigatorContext(sourceObject);
                        String procName = ((DBSProcedure) sourceObject).getName();
                        String title = NLS.bind(PostgreMessages.procedure_check_label2, procName); 
                        try {
                            SQLEditorHandlerOpenObjectConsole.openAndExecuteSQLScript(workbenchWindow, navContext, 
                                title, true, null, sql, true);
                        } catch (CoreException e) {
                            DBWorkbench.getPlatformUI().showError(PostgreMessages.message_open_console, 
                                PostgreMessages.error_cant_open_sql_editor, e);
                        }
                    }
                }, true));
        }
    }

    @Override
    protected Map<String, Object> getSourceOptions() {
        Map<String, Object> options = super.getSourceOptions();
        options.put(DBPScriptObject.OPTION_DEBUGGER_SOURCE, isInDebugMode());
        return options;
    }

    private boolean isInDebugMode() {
        return CommonUtils.getBoolean(
            getDatabaseEditorInput().getAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE), false);
    }
}

