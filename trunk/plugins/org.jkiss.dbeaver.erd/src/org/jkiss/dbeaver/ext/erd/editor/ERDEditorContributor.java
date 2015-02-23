/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorContributor;

/**
 * SQL Editor contributor
 */
public class ERDEditorContributor extends EditorActionBarContributor
{
    private ERDEditorPart activeEditorPart;


    public ERDEditorContributor()
    {
        super();

        createActions();
    }

    private void createActions()
    {
    }

    @Override
    public void dispose()
    {
        setActiveEditor(null);

        super.dispose();
    }

    @Override
    public void setActiveEditor(IEditorPart targetEditor)
    {
        super.setActiveEditor(targetEditor);

        if (activeEditorPart == targetEditor) {
            return;
        }
        activeEditorPart = (ERDEditorPart)targetEditor;

        if (activeEditorPart != null) {
            // Update editor actions


        }
        EntityEditorContributor.registerSearchActions(activeEditorPart);
    }

    @Override
    public void init(IActionBars bars)
    {
        super.init(bars);
    }

    @Override
    public void contributeToMenu(IMenuManager manager)
    {
        super.contributeToMenu(manager);

        IMenuManager editMenu = manager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
        if (editMenu != null) {
            //editMenu.add(new Separator());
            //editMenu.add(new Separator());
            //editMenu.add(executeStatementAction);
            //editMenu.add(executeScriptAction);
        }
    }

    @Override
    public void contributeToCoolBar(ICoolBarManager manager)
    {
        super.contributeToCoolBar(manager);
    }

    @Override
    public void contributeToStatusLine(IStatusLineManager statusLineManager)
    {
        super.contributeToStatusLine(statusLineManager);
    }

}
