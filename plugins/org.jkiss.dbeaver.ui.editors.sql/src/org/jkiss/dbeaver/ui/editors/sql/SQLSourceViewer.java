/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Display Source text (Read Only)
 */
public class SQLSourceViewer<T extends DBPScriptObject & DBSObject> extends SQLEditorNested<T> {

    private IAction OPEN_CONSOLE_ACTION = new Action("Open in SQL console", DBeaverIcons.getImageDescriptor(UIIcon.SQL_CONSOLE)) {
        @Override
        public void run() 
        {
            String sqlText = getDocument().get();
            ISelection selection = getSelectionProvider().getSelection();
            if (selection instanceof TextSelection) {
                if (((TextSelection) selection).getLength() > 0) {
                    sqlText = ((TextSelection) selection).getText();
                }
            }
            final DBPDataSource dataSource = getDataSource();
            String consoleName = getSourceViewerType();
            T sourceObject = getSourceObject();
            if (sourceObject != null) {
                consoleName += " of <" + DBUtils.getObjectFullName(sourceObject, DBPEvaluationContext.UI) + ">";
            }
            SQLEditorHandlerOpenEditor.openSQLConsole(
                UIUtils.getActiveWorkbenchWindow(),
                new SQLNavigatorContext(dataSource),
                consoleName,
                sqlText
            );
        }
    };

    protected String getSourceViewerType() {
        return "DDL";
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException
    {
        return getSourceObject().getObjectDefinitionText(monitor, getSourceOptions());
    }

    protected Map<String, Object> getSourceOptions() {
        IEditorInput editorInput = getEditorInput();
        Map<String, Object> options = new HashMap<>();
        if (editorInput instanceof IDatabaseEditorInput) {
            Collection<String> attributeNames = ((IDatabaseEditorInput)editorInput).getAttributeNames();
            options.put(DBPScriptObject.OPTION_DDL_SOURCE, true);
            if (!attributeNames.isEmpty()) {
                for (String name : attributeNames) {
                    Object attribute = ((IDatabaseEditorInput)editorInput).getAttribute(name);
                    options.put(name, attribute);
                }
            }
        }
        T genObject = getSourceObject();
        if (genObject instanceof DBPScriptObjectExt2) {
            DBPScriptObjectExt2 sourceObject = (DBPScriptObjectExt2) genObject;
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS)) {
                options.put(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS, getShowFullDDL());
            }
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS)) {
                options.put(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS, getShowPermissions());
            }
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_COMMENTS)) {
                options.put(DBPScriptObject.OPTION_INCLUDE_COMMENTS, getShowColumnComments());
            }
        }
        return options;
    }

    @Override
    protected boolean isReadOnly()
    {
        return true;
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText)
    {
    }

    @Override
    protected void contributeEditorCommands(IContributionManager toolBarManager) {
        super.contributeEditorCommands(toolBarManager);
        toolBarManager.add(new Separator());
        toolBarManager.add(OPEN_CONSOLE_ACTION);

        T genObject = getSourceObject();
        if (genObject instanceof DBPScriptObjectExt2) {
            DBPScriptObjectExt2 sourceObject = (DBPScriptObjectExt2) genObject;
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS)) {
                toolBarManager.add(ActionUtils.makeActionContribution(
                        new SQLActionCheckBox("Show full DDL", "Show DDL for all schema objects",
                                DBIcon.TREE_TABLE_EXTERNAL, getShowFullDDL(), DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS),
                        true));
            }
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS)) {
                toolBarManager.add(ActionUtils.makeActionContribution(
                        new SQLActionCheckBox("Show permissions", "Shows object permission grants",
                                DBIcon.TREE_PERMISSIONS, getShowPermissions(), DBPScriptObject.OPTION_INCLUDE_PERMISSIONS),
                        true));
            }
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_COMMENTS)) {
                toolBarManager.add(ActionUtils.makeActionContribution(
                        new SQLActionCheckBox("Show comments", "Show column comments in table definition",
                                DBIcon.TYPE_TEXT, getShowColumnComments(), DBPScriptObject.OPTION_INCLUDE_COMMENTS),
                        true));
            }
        }
    }

    private class SQLActionCheckBox extends Action {
        private final String option;

        public SQLActionCheckBox(String text, String toolTipText, DBPImage image,
                                 boolean checked, String option) {
            super(text, Action.AS_CHECK_BOX);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(image));
            setToolTipText(toolTipText);
            setChecked(checked);
            this.option = option;
        }

        @Override
        public void run() {
            getPreferenceStore().setValue(option, isChecked());
            refreshPart(SQLSourceViewer.this, true);
        }
    }

    protected boolean getShowPermissions() {
        return getPreferenceStore().getBoolean(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS);
    }

    protected boolean getShowColumnComments() {
        return getPreferenceStore().getBoolean(DBPScriptObject.OPTION_INCLUDE_COMMENTS);
    }

    protected boolean getShowFullDDL() {
        return getPreferenceStore().getBoolean(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS);
    }
}
