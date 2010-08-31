/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 23, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.jface.action.Action;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditor;
import org.jkiss.dbeaver.ext.erd.Activator;

/**
 * Action to toggle the layout between manual and automatic
 *
 * @author Phil Zoio
 */
public class DiagramLayoutAction extends Action
{
	private ERDEditor editor;

	public DiagramLayoutAction(ERDEditor editor)
	{
		super("Arrange Diagram", Activator.getImageDescriptor("icons/arrangeall.png"));
		this.editor = editor;
	}

	public void run()
	{
        //editor.get
        editor.getDiagramPart().rearrangeDiagram();
	}

}