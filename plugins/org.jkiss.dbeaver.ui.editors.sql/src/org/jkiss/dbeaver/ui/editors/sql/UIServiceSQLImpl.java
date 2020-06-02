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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRCreator;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.GenerateSQLParametrizedDialog;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.ViewSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SQLEditorControl
 */
public class UIServiceSQLImpl implements UIServiceSQL {

    private static final Log log = Log.getLog(UIServiceSQLImpl.class);

    @Override
    public int openSQLViewer(DBCExecutionContext context, String title, DBPImage image, String text, boolean showSaveButton, boolean showOpenEditorButton) {
        ViewSQLDialog dialog = new ViewSQLDialog(
            UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
            () -> context,
            title,
            image,
            text
        );
        dialog.setShowSaveButton(showSaveButton);
        dialog.setShowOpenEditorButton(showOpenEditorButton);
        return dialog.open();
    }

    @Override
    public String openSQLEditor(@Nullable DBPContextProvider contextProvider, String title, @Nullable DBPImage image, String text) {
        ViewSQLDialog dialog = new ViewSQLDialog(
            UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
            contextProvider,
            title,
            image,
            text
        );
        dialog.setReadOnly(false);
        if (dialog.open() == IDialogConstants.OK_ID) {
            return dialog.getText();
        }
        return null;
    }

    @Override
    public int openGeneratedScriptViewer(
        @Nullable DBCExecutionContext context,
        String title,
        @Nullable DBPImage image,
        @NotNull DBRCreator<String, Map<String, Object>> scriptGenerator,
        @NotNull DBPPropertyDescriptor[] properties,
        boolean showSaveButton)
    {
        GenerateSQLParametrizedDialog dialog = new GenerateSQLParametrizedDialog(
            UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
            context,
            title,
            image)
        {
            @Override
            protected void createControls(Composite parent) {
                for (DBPPropertyDescriptor prop : properties) {
                    if (prop.getDataType() == Boolean.class) {
                        UIUtils.createCheckbox(parent, prop.getDisplayName(), CommonUtils.notEmpty(prop.getDescription()), false, 1);
                    }
                }
                super.createControls(parent);
            }

            @Override
            protected String[] generateSQLScript() {
                Map<String, Object> params = new LinkedHashMap<>();
                return new String[] { scriptGenerator.createObject(params) };
            }
        };
/*
        ViewSQLDialog dialog = new ViewSQLDialog(
            UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
            context,
            title,
            image,
            text
        );
        dialog.setShowSaveButton(showSaveButton);
        return dialog.open();
*/
        return 0;
    }

    @Override
    public Object openSQLConsole(@NotNull DBPDataSourceContainer dataSourceContainer, DBCExecutionContext executionContext, String name, String sqlText) {
        return SQLEditorHandlerOpenEditor.openSQLConsole(
            UIUtils.getActiveWorkbenchWindow(),
            executionContext != null ? new SQLNavigatorContext(executionContext) : new SQLNavigatorContext(dataSourceContainer),
            name,
            sqlText);
    }

    @Override
    public Object createSQLPanel(Object site, Object parent, DBPContextProvider contextProvider, String panelName, boolean showVerticalBar, String sqlText) throws DBException {
        IWorkbenchPartSite partSite = (IWorkbenchPartSite) site;
        Composite editorPH = (Composite)parent;
        final SQLEditorBase editor = new SQLEditorBase() {
            @Nullable
            @Override
            public DBCExecutionContext getExecutionContext() {
                return contextProvider.getExecutionContext();
            }

            @Override
            public void createPartControl(Composite parent) {
                super.createPartControl(parent);
                getAction(ITextEditorActionConstants.CONTEXT_PREFERENCES).setEnabled(false);
            }

            @Override
            public boolean isFoldingEnabled() {
                return false;
            }
        };
        editor.setHasVerticalRuler(showVerticalBar);
        try {
            editor.init(new SubEditorSite(partSite), new StringEditorInput(panelName, sqlText, true, GeneralUtils.getDefaultFileEncoding()));
        } catch (PartInitException e) {
            throw new DBException("Error initializing SQL panel", e);
        }
        editor.createPartControl(editorPH);
        editor.reloadSyntaxRules();

        TextViewer textViewer = editor.getTextViewer();
        textViewer.setData("editor", editor);
        TextEditorUtils.enableHostEditorKeyBindingsSupport(partSite, textViewer.getTextWidget());

        return textViewer;
    }

    @Override
    public void setSQLPanelText(Object panelObject, String sqlText) {
        if (panelObject instanceof TextViewer) {
            Object editor = ((TextViewer) panelObject).getData("editor");
            if (editor instanceof SQLEditorBase) {
                ((SQLEditorBase) editor).setInput(
                    new StringEditorInput("SQL", sqlText, true, GeneralUtils.getDefaultFileEncoding()));
                ((SQLEditorBase) editor).reloadSyntaxRules();
            }
        }
    }

    @Override
    public String getSQLPanelText(Object panelObject) {
        if (panelObject instanceof TextViewer) {
            Object editor = ((TextViewer) panelObject).getData("editor");
            if (editor instanceof SQLEditorBase) {
                return ((SQLEditorBase) editor).getDocument().get();
            }
        }
        return null;
    }

    @Override
    public void disposeSQLPanel(Object panelObject) {
        if (panelObject instanceof TextViewer) {
            Object editor = ((TextViewer) panelObject).getData("editor");
            if (editor instanceof SQLEditorBase) {
                UIUtils.asyncExec(((SQLEditorBase) editor)::dispose);
            }
        }
    }

    @Override
    public Object openNewScript(DBSObject forObject) {
        try {
            SQLEditorHandlerOpenEditor.openNewEditor(new SQLNavigatorContext(forObject), null);
            return true;
        } catch (CoreException e) {
            DBWorkbench.getPlatformUI().showError("Open new SQL editor", "Can't open new SQL editor", e);
            return false;
        }
    }

    @Override
    public Object openRecentScript(DBSObject forObject) {
        try {
            SQLEditorHandlerOpenEditor.openRecentScript(
                UIUtils.getActiveWorkbenchWindow(),
                new SQLNavigatorContext(forObject),
                null);
            return true;
        } catch (CoreException e) {
            DBWorkbench.getPlatformUI().showError("Open SQL editor", "Can't open SQL editor", e);
            return false;
        }
    }

    @Override
    public void openResource(IResource element) {
        SQLEditorHandlerOpenEditor.openResource(element, new SQLNavigatorContext());
    }

    @Override
    public boolean useIsolatedConnections(DBPContextProvider contextProvider) {
        return contextProvider.getExecutionContext().getDataSource().getContainer().getPreferenceStore().getBoolean(SQLPreferenceConstants.EDITOR_SEPARATE_CONNECTION);
    }
}
