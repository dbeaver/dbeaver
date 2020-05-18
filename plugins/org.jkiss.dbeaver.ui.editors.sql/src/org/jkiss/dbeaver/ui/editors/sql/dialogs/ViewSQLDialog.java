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
package org.jkiss.dbeaver.ui.editors.sql.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.internal.ResultSetMessages;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLEditorHandlerOpenEditor;
import org.jkiss.dbeaver.ui.editors.sql.handlers.SQLNavigatorContext;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;

public class ViewSQLDialog extends BaseSQLDialog {

    private static final String DIALOG_ID = "DBeaver.ViewSQLDialog";//$NON-NLS-1$

    private DBPContextProvider contextProvider;
    private String text;
    private boolean showSaveButton = false;
    private boolean enlargeViewPanel = true;
    private boolean wordWrap = false;
    private boolean showOpenEditorButton;

    public ViewSQLDialog(final IWorkbenchPartSite parentSite, @Nullable DBPContextProvider contextProvider, String title, @Nullable DBPImage image, String text)
    {
        super(parentSite, title, image);
        this.contextProvider = contextProvider;
        this.text = text;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings()
    {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    public void setShowSaveButton(boolean showSaveButton)
    {
        this.showSaveButton = showSaveButton;
    }

    public void setEnlargeViewPanel(boolean enlargeViewPanel) {
        this.enlargeViewPanel = enlargeViewPanel;
    }

    public void setShowOpenEditorButton(boolean showOpenEditorButton) {
        this.showOpenEditorButton = showOpenEditorButton;
    }

    @Override
    protected boolean isWordWrap() {
        return wordWrap;
    }

    public void setWordWrap(boolean wordWrap) {
        this.wordWrap = wordWrap;
    }

    @Override
    protected Control createContents(Composite parent) {
        Control contents = super.createContents(parent);

        Button closeButton = getButton(showSaveButton ? IDialogConstants.PROCEED_ID : IDialogConstants.OK_ID);
        if (closeButton != null) {
            UIUtils.asyncExec(closeButton::setFocus);
        }

        return contents;
    }

    @Override
    protected Composite createDialogArea(Composite parent)
    {
        Composite composite = super.createDialogArea(parent);
        Composite sqlPanel = createSQLPanel(composite);
        GridData gd = (GridData) sqlPanel.getLayoutData();
        if (enlargeViewPanel) {
            gd.widthHint = 500;
            gd.heightHint = 400;
        } else {
            gd.widthHint = 400;
            gd.heightHint = 100;
        }
        return sqlPanel;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent)
    {
        if (showOpenEditorButton) {
            createButton(parent, IDialogConstants.OPEN_ID, ResultSetMessages.dialog_text_view_open_editor, true);
        }
        if (showSaveButton) {
            createButton(parent, IDialogConstants.PROCEED_ID, SQLEditorMessages.dialog_view_sql_button_persist, true);
            createCopyButton(parent);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        } else {
            if (isReadOnly()) {
                createCopyButton(parent);
                createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
            } else {
                // Standard OK/Cancel
                super.createButtonsForButtonBar(parent);
            }
        }
    }

    @Override
    protected String getSQLText()
    {
        return text;
    }

    public void setSQLText(String text) {
        this.text = text;
    }

    @Override
    protected void buttonPressed(int buttonId)
    {
        if (buttonId == IDialogConstants.OPEN_ID) {
            String title = getTitle();
            String text = getText();
            UIUtils.asyncExec(() ->
                {
                    SQLEditorHandlerOpenEditor.openSQLConsole(
                        UIUtils.getActiveWorkbenchWindow(),
                        new SQLNavigatorContext(contextProvider.getExecutionContext()),
                        title,
                        text);
                }
            );
            close();
        } else if (buttonId == IDialogConstants.PROCEED_ID) {
            setReturnCode(IDialogConstants.PROCEED_ID);
            close();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected DBCExecutionContext getExecutionContext() {
        return contextProvider.getExecutionContext();
    }

}