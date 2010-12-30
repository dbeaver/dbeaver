/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Aug 12, 2004
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.EditDomain;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;

/**
 * PaletteViewerProvider subclass used for initialising drag and drop
 * @author Serge Rieder
 */
public class ERDPaletteViewerProvider extends PaletteViewerProvider
{

	/**
	 * implicit constructor
	 */
	public ERDPaletteViewerProvider(EditDomain graphicalViewerDomain)
	{
		super(graphicalViewerDomain);
	}

	protected void configurePaletteViewer(PaletteViewer viewer)
	{
		super.configurePaletteViewer(viewer);
		viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
	}

}