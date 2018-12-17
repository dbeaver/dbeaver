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

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.ViewSQLDialog;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.handlers.OpenHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * SQLEditorControl
 */
public class UIServiceSQLImpl implements UIServiceSQL {

    private static final Log log = Log.getLog(UIServiceSQLImpl.class);

    @Override
    public void openSQLViewer(DBCExecutionContext context, String title, DBPImage image, String text) {
        ViewSQLDialog dialog = new ViewSQLDialog(
            UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite(),
            context,
            title,
            image,
            text
        );
        dialog.open();
    }

    @Override
    public Object openSQLConsole(DBPDataSourceContainer dataSourceContainer, String name, String sqlText) {
        return OpenHandler.openSQLConsole(
            UIUtils.getActiveWorkbenchWindow(),
            dataSourceContainer,
            name,
            sqlText);
    }

    @Override
    public Object createSQLPanel(Object site, Object parent, DBPContextProvider contextProvider, String panelName, String sqlText) throws DBException {
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
        editor.setHasVerticalRuler(false);
        try {
            editor.init(new SubEditorSite(partSite), new StringEditorInput(panelName, sqlText, true, GeneralUtils.getDefaultFileEncoding()));
        } catch (PartInitException e) {
            throw new DBException("Error initializing SQL panel", e);
        }
        editor.createPartControl(editorPH);
        editor.reloadSyntaxRules();

        editorPH.addDisposeListener(e -> editor.dispose());

        return editor.getTextViewer();
    }


}
