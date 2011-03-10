/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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