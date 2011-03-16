/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 22, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.ui.actions.ActionFactory;
import org.jkiss.dbeaver.ext.erd.action.DiagramLayoutAction;

/**
 * Provides a context menu for the schema diagram editor. A virtual cut and paste from the flow example
 * @author Daniel Lee
 */
public class ERDEditorContextMenuProvider extends ContextMenuProvider
{
    private ERDEditorPart editor;
	private ActionRegistry actionRegistry;

	/**
	 * Creates a new FlowContextMenuProvider assoicated with the given viewer
	 * and action registry.
	 * 
	 * @param editor
	 *            the editor
	 * @param registry
	 *            the action registry
	 */
	public ERDEditorContextMenuProvider(ERDEditorPart editor, ActionRegistry registry)
	{
		super(editor.getViewer());
        this.editor = editor;
		setActionRegistry(registry);
	}

	/**
	 * @see ContextMenuProvider#buildContextMenu(org.eclipse.jface.action.IMenuManager)
	 */
	public void buildContextMenu(IMenuManager menu)
	{
		GEFActionConstants.addStandardActionGroups(menu);

		IAction action;
		action = getActionRegistry().getAction(ActionFactory.UNDO.getId());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, action);

		action = getActionRegistry().getAction(ActionFactory.REDO.getId());
		menu.appendToGroup(GEFActionConstants.GROUP_UNDO, action);

		action = getActionRegistry().getAction(ActionFactory.DELETE.getId());
		if (action.isEnabled())
			menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);

        action = getActionRegistry().getAction(ActionFactory.SELECT_ALL.getId());
        if (action.isEnabled())
            menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);

        action = getActionRegistry().getAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY);
        if (action.isEnabled())
            menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);

        action = new DiagramLayoutAction(editor);
        menu.appendToGroup(GEFActionConstants.GROUP_VIEW, action);
        //action = getActionRegistry().getAction(ActionFactory.PRINT.getId());
        //menu.appendToGroup(GEFActionConstants.GROUP_PRINT, action);
        //action = getActionRegistry().getAction(GEFActionConstants.TOGGLE_GRID_VISIBILITY);
        //.menu.appendToGroup(GEFActionConstants.GROUP_EDIT, action);

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