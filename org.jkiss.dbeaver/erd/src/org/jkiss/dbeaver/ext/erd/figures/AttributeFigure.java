/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.figures;

import org.eclipse.draw2d.AbstractBorder;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.FlowLayout;
import org.eclipse.draw2d.Graphics;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Insets;
import org.jkiss.dbeaver.ext.erd.model.ERDTable;

/**
 * Figure used to hold the column labels
 * @author Serge Rieder
 */
public class AttributeFigure extends Figure
{

	public AttributeFigure(ERDTable table)
	{
		FlowLayout layout = new FlowLayout();
		layout.setMinorAlignment(FlowLayout.ALIGN_LEFTTOP);
		layout.setStretchMinorAxis(false);
		layout.setHorizontal(false);
		setLayoutManager(layout);
		setBorder(new ColumnFigureBorder());
        if (table.isPrimary()) {
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

		public Insets getInsets(IFigure figure)
		{
			return new Insets(5, 3, 3, 1);
		}

		public void paint(IFigure figure, Graphics graphics, Insets insets)
		{
			graphics.drawLine(getPaintRectangle(figure, insets).getTopLeft(), tempRect.getTopRight());
		}
	}
}