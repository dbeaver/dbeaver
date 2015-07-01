/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
/*
 * Created on Jul 22, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.jkiss.dbeaver.ext.erd.action.DiagramLayoutAction;

/**
 * Provides a context menu for the schema diagram editor. A virtual cut and paste from the flow example
 * @author Daniel Lee
 */
public class ERDEditorContextMenuProvider extends ContextMenuProvider
{
    private ERDEditorPart editor;

	/**
	 * Creates a new FlowContextMenuProvider associated with the given viewer
	 * and action registry.
	 * 
	 * @param editor the editor
	 */
	public ERDEditorContextMenuProvider(ERDEditorPart editor)
	{
		super(editor.getViewer());
        this.editor = editor;
	}

	/**
	 * @see ContextMenuProvider#buildContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	@Override
    public void buildContextMenu(IMenuManager menu)
	{
        if (editor.isLoaded()) {
            GEFActionConstants.addStandardActionGroups(menu);

            menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

            menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new Separator());
            menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, editor.createAttributeVisibilityMenu());
            menu.appendToGroup(IWorkbenchActionConstants.MB_ADDITIONS, new DiagramLayoutAction(editor));
        }
	}
}