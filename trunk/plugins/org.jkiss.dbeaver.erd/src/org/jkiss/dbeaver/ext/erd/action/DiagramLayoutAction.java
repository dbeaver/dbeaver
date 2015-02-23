/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
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
/*
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.ext.erd.Activator;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;

/**
 * Action to toggle the layout between manual and automatic
 *
 * @author Serge Rieder
 */
public class DiagramLayoutAction extends Action
{
	private ERDEditorPart editor;

	public DiagramLayoutAction(ERDEditorPart editor)
	{
		super(ERDMessages.action_diagram_layout_name, Activator.getImageDescriptor("icons/arrangeall.png")); //$NON-NLS-2$
		this.editor = editor;
	}

	@Override
    public void run()
	{
        //editor.get
        editor.getDiagramPart().rearrangeDiagram();
	}

}