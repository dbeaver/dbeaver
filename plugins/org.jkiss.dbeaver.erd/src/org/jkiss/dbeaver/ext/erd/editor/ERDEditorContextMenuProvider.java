/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 22, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IAction;
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
	public void buildContextMenu(IMenuManager menu)
	{
		GEFActionConstants.addStandardActionGroups(menu);

		IAction action = new DiagramLayoutAction(editor);
        menu.appendToGroup(GEFActionConstants.GROUP_VIEW, action);

        menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
}