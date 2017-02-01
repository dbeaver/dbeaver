/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.FreeformLayer;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ext.erd.ERDConstants;

/**
 * Figure which represents the whole diagram - the view which corresponds to the
 * Schema model object
 * @author Serge Rider
 */
public class EntityDiagramFigure extends FreeformLayer
{

	public EntityDiagramFigure()
	{
		//setOpaque(true);
        //setChildrenOrientation(Orientable.HORIZONTAL);
		ColorRegistry colorRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();

		setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_DIAGRAM_BACKGROUND));
	}

}