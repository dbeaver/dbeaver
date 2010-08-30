/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;

import org.jkiss.dbeaver.ext.erd.editor.ERDEditor;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;

/**
 * Action to toggle the layout between manual and automatic
 * 
 * @author Phil Zoio
 */
public class FlyoutChangeLayoutAction extends Action
{

	IEditorPart editor;
	boolean checked;

	public FlyoutChangeLayoutAction(IEditorPart editor)
	{
		super("Automatic Layout", Action.AS_CHECK_BOX);
		this.editor = editor;
	}

	public void run()
	{
		if (editor instanceof ERDEditor)
		{
			ERDEditor schemaEditor = (ERDEditor) editor;
			EntityDiagram entityDiagram = schemaEditor.getSchema();
			boolean isManual = entityDiagram.isLayoutManualDesired();
			entityDiagram.setLayoutManualDesired(!isManual);
			checked = !isManual;
			setChecked(checked);
		}
	}

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

		if (editor instanceof ERDEditor)
		{
			ERDEditor schemaEditor = (ERDEditor) editor;
			EntityDiagram entityDiagram = schemaEditor.getSchema();
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