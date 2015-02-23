/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Insets;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;

/**
 * Figure used to hold the column labels
 * @author Serge Rieder
 */
public class AttributeListFigure extends Figure
{

	public AttributeListFigure(ERDEntity entity, boolean key)
	{
		FlowLayout layout = new FlowLayout();
		layout.setMinorAlignment(FlowLayout.ALIGN_TOPLEFT);
		layout.setStretchMinorAxis(false);
		layout.setHorizontal(false);
		setLayoutManager(layout);
		setBorder(new ColumnFigureBorder());
        if (entity.isPrimary()) {
            //setBackgroundColor(EntityFigure.primaryTableColor);
        } else {
		    //setBackgroundColor(ColorConstants.tooltipBackground);
        }
		ColorRegistry colorRegistry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
        setBackgroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ATTR_BACKGROUND));
		setForegroundColor(colorRegistry.get(ERDConstants.COLOR_ERD_ATTR_FOREGROUND));

		setOpaque(true);
	}

	class ColumnFigureBorder extends AbstractBorder
	{

		@Override
        public Insets getInsets(IFigure figure)
		{
			return new Insets(5, 3, 3, 3);
		}

		@Override
        public void paint(IFigure figure, Graphics graphics, Insets insets)
		{
			graphics.drawLine(getPaintRectangle(figure, insets).getTopLeft(), tempRect.getTopRight());
		}
	}
}