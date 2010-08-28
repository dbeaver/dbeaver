/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 22, 2004
 */
package org.jkiss.dbeaver.ext.erd.action;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.IWorkbenchActionConstants;

/**
 * Provides a context menu for the schema diagram editor. A virtual cut and paste from the flow example
 * @author Daniel Lee
 */
public class SchemaContextMenuProvider extends ContextMenuProvider
{

	private ActionRegistry actionRegistry;

	/**
	 * Creates a new FlowContextMenuProvider assoicated with the given viewer
	 * and action registry.
	 * 
	 * @param viewer
	 *            the viewer
	 * @param registry
	 *            the action registry
	 */
	public SchemaContextMenuProvider(EditPartViewer viewer, ActionRegistry registry)
	{
		super(viewer);
		setActionRegistry(registry);
	}

	/**
	 * @see ContextMenuProvider#buildContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void buildContextMenu(IMenuManager menu)
	{
		GEFActionConstants.addStandardActionGroups(menu);

		IAction action;
		action = getActionRegistry().getAction(GEFActionConstants.UNDO);
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, action);

		action = getActionRegistry().getAction(GEFActionConstants.REDO);
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, action);

		action = getActionRegistry().getAction(IWorkbenchActionConstants.DELETE);
		if (action.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);

	}

	private ActionRegistry getActionRegistry()
	{
		return actionRegistry;
	}

	/**
	 * Sets the action registry
	 * 
	 * @param registry
	 *            the action registry
	 */
	public void setActionRegistry(ActionRegistry registry)
	{
		actionRegistry = registry;
	}

}