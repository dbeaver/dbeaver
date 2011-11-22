/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

	public void run()
	{
        //editor.get
        editor.getDiagramPart().rearrangeDiagram();
	}

}