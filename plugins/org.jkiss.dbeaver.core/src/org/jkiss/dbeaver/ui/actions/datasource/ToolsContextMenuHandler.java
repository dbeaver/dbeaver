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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.CoreCommands;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.actions.ConnectionCommands;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorCommands;
import org.jkiss.utils.CommonUtils;

public class ToolsContextMenuHandler extends AbstractDataSourceHandler
{
    private MenuManager menuManager = new MenuManager();

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final IWorkbenchPart part = HandlerUtil.getActivePartChecked(event);
        final Shell activeShell = HandlerUtil.getActiveShell(event);
        if (part == null || activeShell == null) {
            return null;
        }
        final Control focusControl = activeShell.getDisplay().getFocusControl();
        if (focusControl == null) {
            return null;
        }
        Point location = ActionUtils.getLocationFromControl(activeShell, focusControl);

        if (menuManager != null) {
            menuManager.dispose();
        }
        menuManager = new MenuManager();
        menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ConnectionCommands.CMD_CONNECT));
        menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ConnectionCommands.CMD_DISCONNECT));
        menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ConnectionCommands.CMD_INVALIDATE));
        menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ConnectionCommands.CMD_READONLY));
        
        if (part instanceof IEditorPart) {
            menuManager.add(new Separator());
            menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ConnectionCommands.CMD_COMMIT));
            menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ConnectionCommands.CMD_ROLLBACK));
            {
                final MenuManager txnMenu = new MenuManager(
                    DBeaverActivator.getPluginResourceBundle().getString("command.org.jkiss.dbeaver.core.transaction_mode.name"));
                txnMenu.add(new DataSourceTransactionModeContributor());
                menuManager.add(txnMenu);
            }
        }
        menuManager.add(new Separator());
        
        if (ActionUtils.isCommandEnabled(SQLEditorCommands.CMD_SQL_EDITOR_OPEN, part.getSite())) {
            menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), SQLEditorCommands.CMD_SQL_EDITOR_OPEN));
/*
            final MenuManager toolsMenu = new MenuManager(
                DBeaverActivator.getPluginResourceBundle().getString("menu.database.tools"));
            toolsMenu.add(new DataSourceToolsContributor());
            menuManager.add(toolsMenu);
*/
        }
        if (part instanceof IEditorPart) {
            menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), CoreCommands.CMD_LINK_EDITOR, CoreMessages.action_menu_tools_find_in_navigator, null));
        }

        final Menu contextMenu = menuManager.createContextMenu(focusControl);
        if (location != null) {
            contextMenu.setLocation(location);
        }
        contextMenu.setVisible(true);
        contextMenu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(MenuEvent e) {
                int keyIndex = 0;
                for (MenuItem item : contextMenu.getItems()) {
                    if (/*item.getMenu() == null && */!CommonUtils.isEmpty(item.getText())) {
                        item.setText(ActionUtils.getLabelWithIndexMnemonic(item.getText(), keyIndex));
                        keyIndex++;
                    }
                }
            }
        });


        return null;
    }
}