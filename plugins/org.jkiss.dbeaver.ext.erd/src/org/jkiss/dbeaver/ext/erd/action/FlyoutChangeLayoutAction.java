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
/*
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;

/**
 * Action to toggle the layout between manual and automatic
 * 
 * @author Serge Rieder
 */
public class FlyoutChangeLayoutAction extends Action
{

	private IEditorPart editor;
	private boolean checked;

	public FlyoutChangeLayoutAction(IEditorPart editor)
	{
		super("Automatic Layout", Action.AS_CHECK_BOX);
		this.editor = editor;
	}

	@Override
    public void run()
	{
		if (editor instanceof ERDEditorPart)
		{
			ERDEditorPart erdEditor = (ERDEditorPart) editor;
			EntityDiagram entityDiagram = erdEditor.getDiagram();
			boolean isManual = entityDiagram.isLayoutManualDesired();
			entityDiagram.setLayoutManualDesired(!isManual);
            erdEditor.getDiagramPart().changeLayout();

			checked = !isManual;
			setChecked(checked);
		}
	}

	@Override
    public boolean isChecked()
	{
		if (editor != null)
			return isChecked(editor);
		else
			return super.isChecked();
	}

	/**
	 * @see org.eclipse.jface.action.IAction#isChecked()
	 */
	public boolean isChecked(IEditorPart editor)
	{

		if (editor instanceof ERDEditorPart)
		{
			ERDEditorPart schemaEditor = (ERDEditorPart) editor;
			EntityDiagram entityDiagram = schemaEditor.getDiagram();
			boolean checkTrue = entityDiagram.isLayoutManualDesired();
			return (!checkTrue);
		}
		else
		{
			return false;
		}

	}

	public void setActiveEditor(IEditorPart editor)
	{
		this.editor = editor;
		boolean localChecked = isChecked(editor);
		
		//there appears to be a bug in the framework which necessitates this
		if (localChecked)
			firePropertyChange(CHECKED, Boolean.FALSE, Boolean.TRUE);
		else
			firePropertyChange(CHECKED, Boolean.TRUE, Boolean.FALSE);
	}

}