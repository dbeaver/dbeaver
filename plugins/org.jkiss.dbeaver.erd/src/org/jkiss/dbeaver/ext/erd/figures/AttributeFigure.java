/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.*;
import org.eclipse.draw2d.geometry.Insets;
import org.jkiss.dbeaver.ext.erd.model.ERDEntity;

/**
 * Figure used to hold the column labels
 * @author Serge Rieder
 */
public class AttributeFigure extends Figure
{

	public AttributeFigure(ERDEntity entity)
	{
		FlowLayout layout = new FlowLayout();
		layout.setMinorAlignment(FlowLayout.ALIGN_LEFTTOP);
		layout.setStretchMinorAxis(false);
		layout.setHorizontal(false);
		setLayoutManager(layout);
		setBorder(new ColumnFigureBorder());
        if (entity.isPrimary()) {
            //setBackgroundColor(EntityFigure.primaryTableColor);
        } else {
		    //setBackgroundColor(ColorConstants.tooltipBackground);
        }
        setBackgroundColor(ColorConstants.white);
		setForegroundColor(ColorConstants.black);

		setOpaque(true);
	}

	class ColumnFigureBorder extends AbstractBorder
	{

		@Override
        public Insets getInsets(IFigure figure)
		{
			return new Insets(5, 3, 3, 1);
		}

		@Override
        public void paint(IFigure figure, Graphics graphics, Insets insets)
		{
			graphics.drawLine(getPaintRectangle(figure, insets).getTopLeft(), tempRect.getTopRight());
		}
	}
}