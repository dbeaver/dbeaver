/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.ext.erd.dnd;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.dnd.TemplateTransferDropTargetListener;
import org.eclipse.gef.requests.CreationFactory;

/**
 * Provides a listener for dropping templates onto the editor drawing
 */
public class DataEditDropTargetListener extends TemplateTransferDropTargetListener
{

	public DataEditDropTargetListener(EditPartViewer viewer)
	{
		super(viewer);
	}

	@Override
    protected CreationFactory getFactory(Object template)
	{
		return new DataElementFactory(template);
	}

}

