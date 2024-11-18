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

package org.jkiss.dbeaver.ext.kingbase.ui.editors;


import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.jkiss.dbeaver.ext.kingbase.KingbaseMessages;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseProcedure;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseScriptObject;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseTriggerBase;
import org.jkiss.dbeaver.ext.kingbase.model.KingbaseViewBase;
import org.jkiss.dbeaver.ext.kingbase.ui.editors.sql.handlers.SQLEditorHandlerCheckProcedureConsole;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.utils.CommonUtils;


import java.util.Map;


/**
 * KingbaseourceViewEditor
 */
public class KingbaseSourceViewEditor extends SQLSourceViewer<KingbaseScriptObject> {

    public KingbaseSourceViewEditor() {

    }

    @Override
    protected boolean isReadOnly()
    {
        KingbaseScriptObject sourceObject = getSourceObject();
        if (sourceObject instanceof KingbaseProcedure || sourceObject instanceof KingbaseTriggerBase || sourceObject instanceof KingbaseViewBase) {
            return false;
        }
        return true;
    }

    @Override
    protected boolean isAnnotationRulerVisible() {
        return getSourceObject() instanceof KingbaseProcedure;
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
        KingbaseScriptObject sourceObject = getSourceObject();

        if (sourceObject instanceof KingbaseProcedure) {
            contributionManager.add(new Separator());
            contributionManager.add(ActionUtils.makeActionContribution(
                new Action(KingbaseMessages.source_view_show_header_label, Action.AS_CHECK_BOX) {
                    {
                        setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TREE_PROCEDURE));
                        setToolTipText(KingbaseMessages.source_view_show_header_description);
                        setChecked(!isInDebugMode());
                    }
                    @Override
                    public void run() {
                        getDatabaseEditorInput().setAttribute(DBPScriptObject.OPTION_DEBUGGER_SOURCE, !isChecked());
                        refreshPart(KingbaseSourceViewEditor.this, true);
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

