/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

import java.util.*;

/**
 * Display Source text (Read Only)
 */
public class SQLSourceViewer<T extends DBPScriptObject & DBSObject> extends SQLEditorNested<T> {

    protected Boolean separateFKStmts;
    protected Boolean showPermissions;
    protected Boolean showColumnComments;
    protected Boolean showFullDDL;
    private Boolean showPartitionsDDL;

    private final IAction OPEN_CONSOLE_ACTION = new Action(SQLEditorMessages.source_viewer_open_in_sql_console, DBeaverIcons.getImageDescriptor(UIIcon.SQL_CONSOLE)) {
        @Override
        public void run() 
        {
            IDocument document = getDocument();
            if (document == null) {
                return;
            }
            String sqlText = document.get();
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
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        T sourceObject = getSourceObject();
        if (sourceObject == null) {
            return null;
        } else if (!(sourceObject instanceof DBSEntity)) {
            return sourceObject == null ? null : sourceObject.getObjectDefinitionText(monitor, getSourceOptions());
        } else {
            StringBuilder sql = new StringBuilder(100);
            DBStructUtils.generateTableListDDL(monitor, sql, List.of((DBSEntity)sourceObject), getSourceOptions(), false);
            return sql.toString().trim();
        }
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
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS) &&
                sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS)) {
                options.put(DBPScriptObject.OPTION_DDL_SEPARATE_FOREIGN_KEYS_STATEMENTS, getSeparateFKStmts());
            }
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_COMMENTS)) {
                options.put(DBPScriptObject.OPTION_INCLUDE_COMMENTS, getShowColumnComments());
            }
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_PARTITIONS)) {
                options.put(DBPScriptObject.OPTION_INCLUDE_PARTITIONS, getShowPartitionsDDL());
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
    public void activatePart() {
        super.activatePart();

        T sourceObject = getSourceObject();
        if (sourceObject != null && !sourceObject.isPersisted() && this.isReadOnly()) {
            refreshPart(this, true);
        }
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
                        new Action(SQLEditorMessages.source_viewer_show_ddl_text, Action.AS_CHECK_BOX) {
                            {
                                setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TREE_TABLE_EXTERNAL));
                                setToolTipText(SQLEditorMessages.source_viewer_show_ddl_tip);
                                setChecked(getShowFullDDL());
                            }

                            @Override
                            public void run() {
                                showFullDDL = isChecked();
                                getPreferenceStore().setValue(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS, showFullDDL);
                                refreshPart(SQLSourceViewer.this, true);
                            }
                        }, true));
            }
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS)) {
                toolBarManager.add(ActionUtils.makeActionContribution(
                        new Action(SQLEditorMessages.source_viewer_show_permissions_text, Action.AS_CHECK_BOX) {
                            {
                                setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TREE_PERMISSIONS));
                                setToolTipText(SQLEditorMessages.source_viewer_show_permissions_tip);
                                setChecked(getShowPermissions());
                            }

                            @Override
                            public void run() {
                                showPermissions = isChecked();
                                getPreferenceStore().setValue(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS, showPermissions);
                                refreshPart(SQLSourceViewer.this, true);
                            }
                        }, true));
            }
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_DDL_SKIP_FOREIGN_KEYS) &&
                sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_DDL_ONLY_FOREIGN_KEYS)) {
                toolBarManager.add(ActionUtils.makeActionContribution(
                    new Action(SQLEditorMessages.source_viewer_separate_fk_text, Action.AS_CHECK_BOX) {
                        {
                            setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TREE_FOREIGN_KEY));
                            setToolTipText(SQLEditorMessages.source_viewer_separate_fk_tip);
                            setChecked(getSeparateFKStmts());
                        }

                        @Override
                        public void run() {
                            separateFKStmts = isChecked();
                            getPreferenceStore().setValue(DBPScriptObject.OPTION_DDL_SEPARATE_FOREIGN_KEYS_STATEMENTS, separateFKStmts);
                            refreshPart(SQLSourceViewer.this, true);
                        }
                    }, true));
            }
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_COMMENTS)) {
                toolBarManager.add(ActionUtils.makeActionContribution(
                        new Action(SQLEditorMessages.source_viewer_show_comments_text, Action.AS_CHECK_BOX) {
                            {
                                setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TYPE_TEXT));
                                setToolTipText(SQLEditorMessages.source_viewer_show_comments_tip);
                                setChecked(getShowColumnComments());
                            }

                            @Override
                            public void run() {
                                showColumnComments = isChecked();
                                getPreferenceStore().setValue(DBPScriptObject.OPTION_INCLUDE_COMMENTS, showColumnComments);
                                refreshPart(SQLSourceViewer.this, true);
                            }
                        }, true));
            }
            if (sourceObject.supportsObjectDefinitionOption(DBPScriptObject.OPTION_INCLUDE_PARTITIONS)) {
                toolBarManager.add(ActionUtils.makeActionContribution(
                    new Action(SQLEditorMessages.source_viewer_show_partitions_ddl_text, Action.AS_CHECK_BOX) {
                        {
                            setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TREE_PARTITION));
                            setToolTipText(SQLEditorMessages.source_viewer_show_partitions_ddl_tip);
                            setChecked(getShowPartitionsDDL());
                        }

                        @Override
                        public void run() {
                            showPartitionsDDL = isChecked();
                            getPreferenceStore().setValue(DBPScriptObject.OPTION_INCLUDE_PARTITIONS, showPartitionsDDL);
                            refreshPart(SQLSourceViewer.this, true);
                        }
                    }, true));
            }
        }
    }

    protected boolean getShowPermissions() {
        if (showPermissions == null) {
            showPermissions = getPreferenceStore().getBoolean(DBPScriptObject.OPTION_INCLUDE_PERMISSIONS);
        }
        return showPermissions;
    }

    protected boolean getSeparateFKStmts() {
        if (separateFKStmts == null) {
            separateFKStmts = getPreferenceStore().getBoolean(DBPScriptObject.OPTION_DDL_SEPARATE_FOREIGN_KEYS_STATEMENTS);
        }
        return separateFKStmts;
    }

    protected Boolean getShowColumnComments() {
        if (showColumnComments == null) {
            showColumnComments = getPreferenceStore().getBoolean(DBPScriptObject.OPTION_INCLUDE_COMMENTS);
        }
        return showColumnComments;
    }

    protected Boolean getShowFullDDL() {
        if (showFullDDL == null) {
            showFullDDL = getPreferenceStore().getBoolean(DBPScriptObject.OPTION_INCLUDE_NESTED_OBJECTS);
        }
        return showFullDDL;
    }

    protected Boolean getShowPartitionsDDL() {
        if (showPartitionsDDL == null) {
            showPartitionsDDL = getPreferenceStore().getBoolean(DBPScriptObject.OPTION_INCLUDE_PARTITIONS);
        }
        return showPartitionsDDL;
    }
}
