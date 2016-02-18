/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ICommandIds;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

public class ToolsMenuHandler extends AbstractDataSourceHandler
{
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
        Point location = getLocationFromControl(activeShell, focusControl);

        MenuManager menuManager = new MenuManager();
        menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ICommandIds.CMD_CONNECT));
        menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ICommandIds.CMD_DISCONNECT));
        menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ICommandIds.CMD_INVALIDATE));
        if (part instanceof IEditorPart) {
            menuManager.add(new Separator());
            menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ICommandIds.CMD_COMMIT));
            menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ICommandIds.CMD_ROLLBACK));
            {
                final MenuManager txnMenu = new MenuManager(
                    DBeaverActivator.getPluginResourceBundle().getString("command.org.jkiss.dbeaver.core.transaction_mode.name"));
                txnMenu.add(new DataSourceTransactionModeContributor());
                menuManager.add(txnMenu);
            }
        }
        menuManager.add(new Separator());
        {
            menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ICommandIds.CMD_SQL_EDITOR_OPEN));
/*
            final MenuManager toolsMenu = new MenuManager(
                DBeaverActivator.getPluginResourceBundle().getString("menu.database.tools"));
            toolsMenu.add(new DataSourceToolsContributor());
            menuManager.add(toolsMenu);
*/
        }
        if (part instanceof IEditorPart) {
            menuManager.add(ActionUtils.makeCommandContribution(part.getSite(), ICommandIds.CMD_LINK_EDITOR, "Find in navigator", null));
        }

        final Menu contextMenu = menuManager.createContextMenu(focusControl);
        if (location != null) {
            contextMenu.setLocation(location);
        }
        contextMenu.setVisible(true);
        contextMenu.addMenuListener(new MenuAdapter() {
            @Override
            public void menuShown(MenuEvent e) {
                int keyIndex = 1;
                for (MenuItem item : contextMenu.getItems()) {
                    if (/*item.getMenu() == null && */!CommonUtils.isEmpty(item.getText())) {
                        item.setText(String.valueOf(keyIndex) + ". " + item.getText());
                        keyIndex++;
                        if (keyIndex >= 10) {
                            break;
                        }
                    }
                }
            }
        });


        return null;
    }

    @Nullable
    private Point getLocationFromControl(Shell activeShell, Control focusControl) {
        Point location = null;
        final Display display = activeShell.getDisplay();
        if (focusControl instanceof Table) {
            final Table table = (Table) focusControl;
            final int selectionIndex = table.getSelectionIndex();
            if (selectionIndex < 0) {
                location = display.map(focusControl, null, table.getLocation());
            } else {
                Rectangle absBounds = display.map(focusControl, null, table.getItem(selectionIndex).getBounds());
                location = new Point(absBounds.x, absBounds.y + table.getItemHeight());
            }
        } else if (focusControl instanceof Tree) {
            final Tree tree = (Tree) focusControl;
            final TreeItem[] selection = tree.getSelection();
            if (ArrayUtils.isEmpty(selection)) {
                location = display.map(focusControl, null, tree.getLocation());
            } else {
                Rectangle absBounds = display.map(focusControl, null, selection[0].getBounds());
                location = new Point(absBounds.x, absBounds.y + tree.getItemHeight());
            }
        } else if (focusControl instanceof StyledText) {
            final StyledText styledText = (StyledText) focusControl;
            final int caretOffset = styledText.getCaretOffset();
            location = styledText.getLocationAtOffset(caretOffset);
            location = display.map(styledText, null, location);
            location.y += styledText.getLineHeight();
        }
        return location;
    }

}