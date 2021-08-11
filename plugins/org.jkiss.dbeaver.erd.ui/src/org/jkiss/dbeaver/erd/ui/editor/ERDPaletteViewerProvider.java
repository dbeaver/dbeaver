/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Created on Aug 12, 2004
 */
package org.jkiss.dbeaver.erd.ui.editor;

import org.eclipse.gef3.EditDomain;
import org.eclipse.gef3.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef3.ui.palette.PaletteViewer;
import org.eclipse.gef3.ui.palette.PaletteViewerProvider;

/**
 * PaletteViewerProvider subclass used for initialising drag and drop
 * @author Serge Rider
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

	@Override
    protected void configurePaletteViewer(PaletteViewer viewer)
	{
		super.configurePaletteViewer(viewer);
		viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
	}

}