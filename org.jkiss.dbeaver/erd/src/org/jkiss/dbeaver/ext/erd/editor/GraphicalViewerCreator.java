/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 15, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.draw2d.ColorConstants;
import org.eclipse.gef.EditPartFactory;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.editparts.ScalableFreeformRootEditPart;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;

import org.jkiss.dbeaver.ext.erd.directedit.StatusLineValidationMessageHandler;
import org.jkiss.dbeaver.ext.erd.dnd.DataEditDropTargetListener;
import org.jkiss.dbeaver.ext.erd.part.factory.SchemaEditPartFactory;

/**
 * Functionality for configuring the GraphicalViewer
 * @author Phil Zoio
 */
public class GraphicalViewerCreator
{

	private KeyHandler sharedKeyHandler;	
	private GraphicalViewer viewer;
	/** the editor's action registry */
	private ActionRegistry actionRegistry;

	private IEditorSite editorSite;
	

	/**
	 * @param editorSite
	 */
	public GraphicalViewerCreator(IEditorSite editorSite)
	{
		this.editorSite = editorSite;
	}

	/**
	 * Creates a new <code>PaletteViewer</code>, configures, registers and
	 * initializes it.
	 * 
	 * @param parent
	 *            the parent composite
	 */
	public void createGraphicalViewer(Composite parent)
	{
		viewer = createViewer(parent);
	}

	/**
	 * @param parent
	 * @return
	 */
	protected GraphicalViewer createViewer(Composite parent)
	{
		
		StatusLineValidationMessageHandler validationMessageHandler = new StatusLineValidationMessageHandler(editorSite);
		GraphicalViewer viewer = new ValidationEnabledGraphicalViewer(validationMessageHandler);
		viewer.createControl(parent);			
		
		// configure the viewer
		viewer.getControl().setBackground(ColorConstants.white);
		viewer.setRootEditPart(new ScalableFreeformRootEditPart());
		viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));

		viewer.addDropTargetListener(new DataEditDropTargetListener(viewer));

		// initialize the viewer with input
		viewer.setEditPartFactory(getEditPartFactory());
			
		return viewer;
		
		
	}
	
	
	/**
	 * Returns the <code>EditPartFactory</code> that the
	 * <code>GraphicalViewer</code> will use.
	 * 
	 * @return the <code>EditPartFactory</code>
	 */
	protected EditPartFactory getEditPartFactory()
	{
		// todo return your EditPartFactory here
		return new SchemaEditPartFactory();
	}
	
	
	/**
	 * @return Returns the viewer.
	 */
	public GraphicalViewer getViewer()
	{
		return viewer;
	}
	

}