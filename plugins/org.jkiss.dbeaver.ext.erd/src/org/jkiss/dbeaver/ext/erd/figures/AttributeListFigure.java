/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

import java.util.ArrayList;
import java.util.List;

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

	public List<AttributeItemFigure> getAttributes() {
		List<AttributeItemFigure> result = new ArrayList<>();
		for (Object child : getChildren()) {
			if (child instanceof AttributeItemFigure) {
				result.add((AttributeItemFigure) child);
			}
		}
		return result;
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