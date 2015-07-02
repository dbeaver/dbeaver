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
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;

/**
 * Action to toggle the layout between manual and automatic
 *
 * @author Serge Rieder
 */
public class DiagramRefreshAction extends Action
{
	private ERDEditorPart editor;

	public DiagramRefreshAction(ERDEditorPart editor)
	{
		super("Refresh Diagram", DBeaverIcons.getImageDescriptor(UIIcon.REFRESH));
		this.editor = editor;
	}

	@Override
    public void run()
	{
        //editor.get
        editor.refreshDiagram();
	}

}