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
package org.jkiss.dbeaver.data.console;

import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ToolBar;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener.PreferenceChangeEvent;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorListenerDefault;


public class ConsoleViewSwitchContributor extends WorkbenchWindowControlContribution {

    public ConsoleViewSwitchContributor() {
        super();
    }

    @Nullable
    private static SQLEditor obtainSqlEditorContext(@NotNull Control control) {
        while (control != null) {
            Object data = control.getData(SQLEditor.VIEW_PART_PROP_NAME);
            if (data instanceof SQLEditor) {
                return (SQLEditor) data;
            }
            control = control.getParent();
        }
        return null;
    }

    @Nullable
    @Override
    protected Control createControl(@NotNull Composite parent) {
        SQLEditor editor = obtainSqlEditorContext(parent);
        if (editor == null) {
            return null;
        }
        ConsoleViewSwitchHandler.watchForEditor(editor);
        
        ToolBar toolBar = new ToolBar(parent, SWT.FLAT);
        Action action = new Action(ConsoleMessages.console_view_action_tooltip, Action.AS_CHECK_BOX) { 
            {
                setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.SQL_CONSOLE));
            }
            
            @Override
            public void run() {
                ConsoleViewSwitchHandler.toggleConsoleViewForEditor(editor);
            }
        };
        editor.addListener(new SQLEditorListenerDefault() {
            @Override
            public void onDataSourceChanged(PreferenceChangeEvent event) {
                if (event == null || event.getProperty().equals(SQLConsoleViewPreferenceConstants.SHOW_CONSOLE_VIEW_BY_DEFAULT)) {
                    boolean isConsoleViewEnabled = ConsoleViewSwitchHandler.isConsoleViewEnabledForEditor(editor);
                    action.setChecked(isConsoleViewEnabled);
                    editor.setConsoleViewOutputEnabled(isConsoleViewEnabled);
                }
            }
        });
        ContributionItem actionContribution = new ActionContributionItem(action);
        actionContribution.fill(toolBar, 0);
        return toolBar;
    }
}

