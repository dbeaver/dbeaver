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
package org.jkiss.dbeaver.ui.editors.sql.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class OpenLinkInWindowHandler extends AbstractHandler implements IElementUpdater {

    private static final String TITLE = "Search selection in web";
    private static final String SEARCH_WEB_ADDRESS_PREFIX = "https://www.google.com/search?q=";

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor == null) {
            DBWorkbench.getPlatformUI().showError(TITLE, "No suitable editor was found for SQL");
            return null;
        }

        ISelection selection = editor.getSelectionProvider().getSelection();
        if (!(selection instanceof TextSelection textSelection) || isSelectedTextNullOrEmpty(selection)) {
            DBWorkbench.getPlatformUI().showError(TITLE, "No text was selected");
            return null;
        }

        String googleLink = textSelection.getText().trim();
        googleLink = URLEncoder.encode(googleLink, StandardCharsets.UTF_8);
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            ShellUtils.launchProgram(SEARCH_WEB_ADDRESS_PREFIX + googleLink);
        } else {
            DBWorkbench.getPlatformUI().showError(TITLE, "Desktop is not supported.");
        }
        return null;
    }
    
    private boolean isSelectedTextNullOrEmpty(ISelection selection) {
        if (selection == null || selection.isEmpty() || !(selection instanceof TextSelection textSelection)) {
            return true;
        }
        
        return CommonUtils.isEmpty(textSelection.getText());
    }
    
    @Override
    public void updateElement(UIElement element, Map parameters) {
        element.setText(SQLEditorMessages.editors_sql_actions_search_selected_text_online);
        element.setTooltip(SQLEditorMessages.editors_sql_actions_search_selected_text_online_tip);
    }
}
