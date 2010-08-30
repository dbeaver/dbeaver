/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.ui.actions.ActionBarContributor;
import org.eclipse.gef.ui.actions.DeleteRetargetAction;
import org.eclipse.gef.ui.actions.RedoRetargetAction;
import org.eclipse.gef.ui.actions.UndoRetargetAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import org.jkiss.dbeaver.ext.erd.Activator;
import org.jkiss.dbeaver.ext.erd.action.FlyoutChangeLayoutAction;
import org.jkiss.dbeaver.ext.erd.editor.ERDEditor;

/**
 * Contributes actions to the Editor
 * 
 * @author Phil Zoio
 */
public class ERDEditorActionBarContributor extends ActionBarContributor
{

	FlyoutChangeLayoutAction changeLayoutAction;
	IEditorPart editor;
	
	protected void buildActions()
	{
		addRetargetAction(new UndoRetargetAction());
		addRetargetAction(new RedoRetargetAction());
		addRetargetAction(new DeleteRetargetAction());
		buildChangeLayoutAction();
		addAction(changeLayoutAction);
	}

	public void contributeToToolBar(IToolBarManager toolBarManager)
	{
		toolBarManager.add(getAction(IWorkbenchActionConstants.UNDO));
		toolBarManager.add(getAction(IWorkbenchActionConstants.REDO));
		toolBarManager.add(changeLayoutAction);
	}

	private void buildChangeLayoutAction()
	{
		changeLayoutAction = new FlyoutChangeLayoutAction(editor);
		changeLayoutAction.setToolTipText("Automatic Layout");
		changeLayoutAction.setId("org.jkiss.dbeaver.ext.erd.action.ChangeLayoutAction");
		changeLayoutAction.setImageDescriptor(create("icons/", "layout.gif"));
		changeLayoutAction.setDisabledImageDescriptor(create("icons/", "layout_disabled.gif"));
	}

	public void setActiveEditor(IEditorPart editor)
	{
		this.editor = editor;
		ERDEditor schemaEditor = (ERDEditor) editor;
		changeLayoutAction.setActiveEditor(editor);
		super.setActiveEditor(editor);
		
	}

	protected void declareGlobalActionKeys()
	{
		//add support for printing
		addGlobalActionKey(IWorkbenchActionConstants.PRINT);
	}

	private static ImageDescriptor create(String iconPath, String name)
	{
		return Activator.getImageDescriptor(iconPath + name);
	}



}