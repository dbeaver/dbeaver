/*
 * hex, a java hex editor
 * Copyright (C) 2006, 2009 Jordi Bergenthal, pestatije(-at_)users.sourceforge.net
 * The official hex site is sourceforge.net/projects/hex
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.jkiss.dbeaver.ui.editors.hex;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * HexEditor contributor. Contributes status bar and menu bar items
 *
 * @author Jordi
 */
public class HexEditorActionBarContributor extends EditorActionBarContributor {


    class MyMenuContributionItem extends ContributionItem {
        MenuItem myMenuItem = null;

        MyMenuContributionItem(String id)
        {
            super(id);
        }

        public void fill(Menu parent, int index)
        {
            boolean textSelected =
                activeEditor != null && activeEditor.getManager().isTextSelected();
            myMenuItem = new MenuItem(parent, SWT.PUSH, index);
            if (menuTrimId.equals(getId())) {
                myMenuItem.setText("Trim");
                myMenuItem.setEnabled(textSelected && !activeEditor.getManager().isOverwriteMode());
                myMenuItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e)
                    {
                        activeEditor.getManager().doTrim();
                    }
                });
            } else if (menuSelectBlock.equals(getId())) {
                myMenuItem.setText("Select &Block...\tCtrl+E");
                myMenuItem.setEnabled(true);
                myMenuItem.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e)
                    {
                        activeEditor.getManager().doSelectBlock();
                    }
                });
            }
        }
    }


    class MyMenuListener implements IMenuListener {
        public void menuAboutToShow(IMenuManager menu)
        {
            boolean textSelected = activeEditor.getManager().isTextSelected();
            boolean lengthModifiable = textSelected && !activeEditor.getManager().isOverwriteMode();
            IActionBars bars = getActionBars();

            IContributionItem contributionItem = bars.getMenuManager().findUsingPath(
                IWorkbenchActionConstants.M_EDIT + '/' + menuTrimId);
            if (contributionItem != null &&
                ((MyMenuContributionItem) contributionItem).myMenuItem != null &&
                !((MyMenuContributionItem) contributionItem).myMenuItem.isDisposed())
                ((MyMenuContributionItem) contributionItem).myMenuItem.setEnabled(lengthModifiable);
        }
    }


    final class MyStatusLineContributionItem extends ContributionItem {
        MyStatusLineContributionItem(String id)
        {
            super(id);
        }

        public void fill(Composite parent)
        {
            if (activeEditor != null)
                activeEditor.getManager().createStatusPart(parent, true);
        }
    }


    static final String menuTrimId = "trim";
    static final String menuSelectBlock = "selectBlock";
    static final String statusLineItemId = "AllHexEditorStatusItemsItem";
    HexEditor activeEditor = null;


    /**
     * @see EditorActionBarContributor#contributeToMenu(org.eclipse.jface.action.IMenuManager)
     */
    public void contributeToMenu(IMenuManager menuManager)
    {
        IMenuListener myMenuListener = new MyMenuListener();

        IMenuManager menu = menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (menu != null) {
            menu.insertAfter("delete", new MyMenuContributionItem(menuTrimId));
            menu.addMenuListener(myMenuListener);
        }

        menu = menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (menu != null) {
            menu.insertAfter("selectAll", new MyMenuContributionItem(menuSelectBlock));
            menu.addMenuListener(myMenuListener);
        }

        menu = menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_NAVIGATE);
        if (menu != null) {
            Action goToAction = new Action() {
                public void run()
                {
                    activeEditor.getManager().doGoTo();
                }
            };
            // declared in org.eclipse.ui.workbench.text plugin.xml
            goToAction.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_GOTO);
            goToAction.setText("Go to &Location...\tCtrl+L");
            menu.appendToGroup("additions", goToAction);
        }
    }


    /**
     * @see EditorActionBarContributor#contributeToStatusLine(org.eclipse.jface.action.IStatusLineManager)
     */
    public void contributeToStatusLine(IStatusLineManager statusLineManager)
    {
        statusLineManager.add(new MyStatusLineContributionItem(statusLineItemId));
    }


    /**
     * @see IEditorActionBarContributor#setActiveEditor(org.eclipse.ui.IEditorPart)
     */
    public void setActiveEditor(IEditorPart targetEditor)
    {
        if (activeEditor != null)
            ((HexEditor) targetEditor).getManager().reuseStatusControlFrom(activeEditor.getManager());
        activeEditor = (HexEditor) targetEditor;
        activeEditor.getManager().setFocus();
        activeEditor.updateActionsStatus();
    }
}
